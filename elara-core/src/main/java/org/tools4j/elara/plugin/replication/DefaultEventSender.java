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
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Int2ObjectHashMap;
import org.tools4j.elara.flyweight.FrameDescriptor;
import org.tools4j.elara.log.IndexTrackingPoller;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.MessageLog.Handler;
import org.tools4j.elara.plugin.replication.Connection.Publisher;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

final class DefaultEventSender implements EventSender {

    private final ReplicationState state;
    private final Publisher publisher;
    private final MutableDirectBuffer sendBuffer;
    private final Int2ObjectHashMap<IndexTrackingPoller> pollerByServerId = new Int2ObjectHashMap<>();
    private final PublishingHandler publishingHandler = new PublishingHandler();

    DefaultEventSender(final Configuration configuration,
                       final ReplicationState state,
                       final MessageLog messageLog,
                       final Publisher publisher) {
        requireNonNull(configuration);
        this.state = requireNonNull(state);
        requireNonNull(messageLog);
        this.publisher = requireNonNull(publisher);
        this.sendBuffer = new ExpandableDirectByteBuffer(Math.max(FrameDescriptor.HEADER_LENGTH, configuration.initialSendBufferCapacity()));
        final int currentServerId = configuration.serverId();
        for (final int serverId : configuration.serverIds()) {
            if (serverId != currentServerId) {
                pollerByServerId.put(serverId, IndexTrackingPoller.create(messageLog));
            }
        }
    }

    @Override
    public boolean sendEvent(final int targetServerId, final long eventLogIndex) {
        final IndexTrackingPoller poller = pollerByServerId.get(targetServerId);
        if (poller == null) {
            throw new NullPointerException("No poller found for target server " + targetServerId);
        }
        final long index = poller.index();
        if (index == eventLogIndex) {
            return poller.poll(publishingHandler.init(targetServerId, eventLogIndex)) > 0;
        }
        if (index < eventLogIndex) {
            poller.moveToNext();
        } else {
            poller.moveToPrevious();
        }
        return false;
    }

    private final class PublishingHandler implements Handler {
        int targetServerId = NULL_SERVER;
        long eventLogIndex = -1;
        PublishingHandler init(final int targetServerId, final long eventLogIndex) {
            this.targetServerId = targetServerId;
            this.eventLogIndex = eventLogIndex;
            return this;
        }

        @Override
        public Result onMessage(final DirectBuffer message) {
            final int length = ReplicationMessages.appendRequest(sendBuffer, 0, state.currentTerm(),
                    state.leaderId(), eventLogIndex, message, 0, message.capacity());
            if (publisher.publish(targetServerId, sendBuffer, 0, length)) {
                return Result.POLL;
            }
            return Result.PEEK;
        }
    }
}
