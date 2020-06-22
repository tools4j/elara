/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.flyweight.FrameDescriptor;
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.plugin.replication.Connection.Publisher;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.APPEND_REQUEST;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.APPEND_RESPONSE;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.logIndex;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.payloadSize;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.type;
import static org.tools4j.elara.plugin.replication.ReplicationMessages.version;

final class ConnectionHandler implements Connection.Handler {

    private final ReplicationLogger logger;
    private final int serverId;
    private final ReplicationState.Volatile state;
    private final Appender eventLogAppender;
    private final Publisher responseSender;
    private final UnsafeBuffer bufferView = new UnsafeBuffer(0, 0);
    private final MutableDirectBuffer sendBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(HEADER_LENGTH));

    ConnectionHandler(final Configuration configuration,
                      final ReplicationState.Volatile state,
                      final Appender eventLogAppender,
                      final Publisher responseSender) {
        this.logger = configuration.replicationLogger();
        this.serverId = configuration.serverId();
        this.state = requireNonNull(state);
        this.eventLogAppender = requireNonNull(eventLogAppender);
        this.responseSender = requireNonNull(responseSender);
    }

    @Override
    public void onMessage(final int senderServerId, final DirectBuffer buffer, final int offset, final int length) {
        bufferView.wrap(buffer, offset, length);
        try {
            final byte type = type(bufferView);
            final byte version = version(bufferView);
            if (version != VERSION) {
                logger.warn("Ignoring message of type {}: version {} found but expected {}", type,
                        version, VERSION);
                return;
            }
            if (isLeader()) {
                if (type != APPEND_RESPONSE) {
                    logger.warn("Ignoring message of type {} in leader mode", type);
                    return;
                }
                if (senderServerId == state.leaderId()) {
                    logger.warn("Ignoring message of type {} in leader mode: response from {} is from myself?!",
                            type, senderServerId);
                    return;
                }
                handleAppendResponse(senderServerId, bufferView);
            } else {
                if (type != APPEND_REQUEST) {
                    logger.warn("Ignoring message of type {} in follower mode", type);
                    return;
                }
                if (senderServerId != state.leaderId()) {
                    logger.warn("Ignoring message of type {} in follower mode: leader is {} but message received from {}",
                            type, state.leaderId(), senderServerId);
                    return;
                }
                handleAppendRequest(senderServerId, bufferView);
            }
        } finally {
            bufferView.wrap(0, 0);
        }
    }

    private void handleAppendRequest(final int senderServerId, final DirectBuffer buffer) {
        final long logIndex = logIndex(buffer);
        long nextEventLogIndex = state.eventLogSize();
        if (logIndex == nextEventLogIndex) {
            final int payloadSize = payloadSize(buffer);
            if (payloadSize < FrameDescriptor.HEADER_LENGTH) {
                logger.warn("Ignoring append-request message in follower mode: payload size {} is smaller than frame header length {}",
                        payloadSize, FrameDescriptor.HEADER_LENGTH);
                return;
            }
            eventLogAppender.append(buffer, PAYLOAD_OFFSET, payloadSize);
            nextEventLogIndex++;
        }
        sendAppendResponse(senderServerId, nextEventLogIndex, logIndex <= nextEventLogIndex);
    }

    private void sendAppendResponse(final int targetServerId,
                                    final long nextEventLogIndex,
                                    final boolean success) {
        final int length = ReplicationMessages.appendResponse(sendBuffer, 0, state.currentTerm(),
                state.leaderId(), nextEventLogIndex, success);
        if (!responseSender.publish(targetServerId, sendBuffer, 0, length)) {
            logger.warn("sending append response to {} for next event log index {} failed", targetServerId, nextEventLogIndex);
        }
    }

    private void handleAppendResponse(final int senderServerId, final DirectBuffer buffer) {
        final boolean appendSuccessful = ReplicationMessages.isAppendSuccess(buffer);
        final long nextEventLogIndex = logIndex(buffer);
        if (!appendSuccessful || nextEventLogIndex > state.nextEventLogIndex(senderServerId)) {
            state.nextEventLogIndex(senderServerId, nextEventLogIndex);
        }
    }

    private boolean isLeader() {
        return serverId == state.leaderId();
    }

}
