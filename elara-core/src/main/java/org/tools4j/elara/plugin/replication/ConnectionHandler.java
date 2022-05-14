/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.tools4j.elara.plugin.replication;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.flyweight.FrameDescriptor;
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.logging.Logger.Level;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.replication.Connection.Publisher;
import org.tools4j.elara.store.MessageStore.Appender;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.APPEND_REQUEST;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.APPEND_RESPONSE;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.payloadSize;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.storeIndex;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.term;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.type;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.version;

final class ConnectionHandler implements Connection.Handler {

    public static final long RESPONSE_DELAY_NANOS = 60;
    public static final long RESEND_DELAY_NANOS = 10_000;

    private final ElaraLogger logger;
    private final int serverId;
    private final BaseState baseState;
    private final ReplicationState.Volatile state;
    private final Appender eventStoreAppender;
    private final Publisher responseSender;
    private final FlyweightEvent flyweightEvent = new FlyweightEvent();
    private final UnsafeBuffer bufferView = new UnsafeBuffer(0, 0);
    private final MutableDirectBuffer sendBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(HEADER_LENGTH));

    ConnectionHandler(final Logger.Factory loggerFactory,
                      final Configuration configuration,
                      final BaseState baseState,
                      final ReplicationState.Volatile state,
                      final Appender eventStoreAppender,
                      final Publisher responseSender) {
        this.logger = ElaraLogger.create(loggerFactory, getClass());
        this.serverId = configuration.serverId();
        this.baseState = requireNonNull(baseState);
        this.state = requireNonNull(state);
        this.eventStoreAppender = requireNonNull(eventStoreAppender);
        this.responseSender = requireNonNull(responseSender);
    }

    @Override
    public void onMessage(final int senderServerId, final DirectBuffer buffer, final int offset, final int length) {
        bufferView.wrap(buffer, offset, length);
        try {
            final byte type = type(bufferView);
            final byte version = version(bufferView);
            if (version != VERSION) {
                logger.warn("Server {}: Ignoring message of type {} from {}: version {} found but expected {}")
                        .replace(serverId).replace(type).replace(senderServerId).replace(version).replace(VERSION).format();
                return;
            }
            if (isLeader()) {
                if (type != APPEND_RESPONSE) {
                    logger.warn("Server {}: Ignoring message of type {} from sender {} in leader mode")
                            .replace(serverId).replace(type).replace(senderServerId).format();
                    return;
                }
                if (senderServerId == state.leaderId()) {
                    logger.warn("Server {}: Ignoring message of type {} in leader mode: response from sender {} is from myself?!")
                            .replace(serverId).replace(type).replace(senderServerId).format();
                    return;
                }
                handleAppendResponse(senderServerId, bufferView);
            } else {
                if (type != APPEND_REQUEST) {
                    logger.warn("Server {}: Ignoring message of type {} from sender {} in follower mode")
                            .replace(serverId).replace(type).replace(senderServerId).format();
                    return;
                }
                handleAppendRequest(senderServerId, bufferView);
            }
        } finally {
            bufferView.wrap(0, 0);
        }
    }

    private void handleAppendRequest(final int senderServerId, final DirectBuffer buffer) {
        final int senderTerm = term(buffer);
        final int currentTerm = state.term();
        if (senderTerm < currentTerm) {
            logger.warn("Server {}: Ignoring append-request message in follower mode: term {} of sender {} is lower than current term {}")
                    .replace(serverId).replace(senderTerm).replace(senderServerId).replace(currentTerm).format();
            return;
        }
        final int leaderId = state.leaderId();
        if (senderTerm == currentTerm && senderServerId != state.leaderId()) {
            logger.warn("Server {}: Ignoring append-request message in follower mode: leader is {} in term {} but message received from sender {}")
                    .replace(serverId).replace(leaderId).replace(currentTerm).replace(senderServerId).format();
            return;
        }
        final long storeIndex = storeIndex(buffer);
        long nextEventStoreIndex = state.eventStoreSize();
        if (storeIndex == nextEventStoreIndex) {
            final int payloadSize = payloadSize(buffer);
            if (payloadSize < FrameDescriptor.HEADER_LENGTH) {
                logger.warn("Server {}: Ignoring append-request message in follower mode: payload size {} is smaller than frame header length {}")
                        .replace(serverId).replace(payloadSize).replace(FrameDescriptor.HEADER_LENGTH).format();
                return;
            }
            flyweightEvent.init(buffer, PAYLOAD_OFFSET);
            if (baseState.eventApplied(flyweightEvent.id())) {
                logger.warn("Server {}: Ignoring append-request message in follower mode: event {}:{}.{} has already been applied")
                        .replace(serverId).replace(flyweightEvent.source()).replace(flyweightEvent.sequence()).replace(flyweightEvent.index()).format();
                return;
            }
            eventStoreAppender.append(buffer, PAYLOAD_OFFSET, payloadSize);
            if (logger.isEnabled(Level.DEBUG)) {
                logger.debug("Server {}: Processed append-request message {} in follower mode")
                        .replace(serverId).replace(nextEventStoreIndex).format();
            }
            nextEventStoreIndex++;
        }
        final boolean success = storeIndex <= nextEventStoreIndex;
        if (!success && logger.isEnabled(Level.DEBUG)) {
            logger.debug("Server {}: Ignoring append-request message in follower mode: expected event store index {} but received {}")
                    .replace(serverId).replace(nextEventStoreIndex).replace(storeIndex).format();
        }
        final long nextSendingTime = success ? 0 : state.nextNotBefore(senderServerId);
        if (nextSendingTime == 0 || System.nanoTime() - nextSendingTime >= 0) {
            final boolean sent = sendAppendResponse(senderServerId, nextEventStoreIndex, success);
            if (success) {
                state.nextNotBefore(senderServerId, 0);
            } else if (sent) {
                state.nextNotBefore(senderServerId, System.nanoTime() + RESPONSE_DELAY_NANOS);
            }
        }
    }

    private boolean sendAppendResponse(final int targetServerId,
                                       final long nextEventStoreIndex,
                                       final boolean success) {
        final int length = ReplicationMessages.appendResponse(sendBuffer, 0, state.term(),
                state.leaderId(), nextEventStoreIndex, success);
        if (!responseSender.publish(targetServerId, sendBuffer, 0, length)) {
            logger.warn("Server {}: Sending append response to {} for next event store index {} failed")
                    .replace(serverId).replace(targetServerId).replace(nextEventStoreIndex).format();
            return false;
        }
        return true;
    }

    private void handleAppendResponse(final int senderServerId, final DirectBuffer buffer) {
        final boolean appendSuccessful = ReplicationMessages.isAppendSuccess(buffer);
        final long nextEventStoreIndex = storeIndex(buffer);
        if (appendSuccessful && nextEventStoreIndex > 0) {
            state.confirmedEventStoreIndex(senderServerId, nextEventStoreIndex - 1);
        }
        if (!appendSuccessful || nextEventStoreIndex > state.nextEventStoreIndex(senderServerId)) {
            if (state.nextEventStoreIndex(senderServerId) != nextEventStoreIndex) {
                state.nextEventStoreIndex(senderServerId, nextEventStoreIndex);
                if (logger.isEnabled(Level.DEBUG)) {
                    logger.debug("Server {}: Reset next event store index to to {} for server {}")
                            .replace(serverId).replace(nextEventStoreIndex).replace(senderServerId).format();
                }
            }
        }
    }

    private boolean isLeader() {
        return serverId == state.leaderId();
    }

}
