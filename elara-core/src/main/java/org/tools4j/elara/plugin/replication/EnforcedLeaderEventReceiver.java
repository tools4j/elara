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

import org.agrona.MutableDirectBuffer;
import org.agrona.collections.IntHashSet;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightHeader;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.log.ExpandableDirectBuffer;
import org.tools4j.elara.log.MessageLog.AppendContext;
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger.Factory;
import org.tools4j.elara.plugin.replication.EnforceLeaderInput.EnforceLeaderReceiver;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationCommands.ENFORCE_LEADER;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.LEADER_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TERM_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

final class EnforcedLeaderEventReceiver implements Receiver.Default, EnforceLeaderReceiver {

    private final ElaraLogger logger;
    private final TimeSource timeSource;
    private final int serverId;
    private final IntHashSet serverIds;
    private final ReplicationState state;
    private final Appender eventLogAppender;
    private final ReceivingContext receivingContext = new ReceivingContext();

    EnforcedLeaderEventReceiver(final Factory loggerFactory,
                                final TimeSource timeSource,
                                final Configuration configuration,
                                final ReplicationState state,
                                final Appender eventLogAppender) {
        this.logger = ElaraLogger.create(loggerFactory, getClass());
        this.timeSource = requireNonNull(timeSource);
        this.serverIds = new IntHashSet(NULL_SERVER);
        this.state = requireNonNull(state);
        this.eventLogAppender = requireNonNull(eventLogAppender);
        this.serverId = configuration.serverId();
        for (final int serverId : configuration.serverIds()) {
            serverIds.add(serverId);
        }
    }

    @Override
    public int serverId() {
        return serverId;
    }

    @Override
    public int currentTerm() {
        return state.currentTerm();
    }

    @Override
    public int leaderId() {
        return state.leaderId();
    }

    @Override
    public void enforceLeader(final int source, final long sequence, final int leaderId) {
        if (!isValidLeaderId(leaderId)) {
            logger.warn("Server {}: Ignoring enforce-leader request {}:{} due to invalid leader ID {}")
                    .replace(serverId).replace(source).replace(sequence).replace(leaderId).format();
            return;
        }
        try (final Receiver.ReceivingContext context = receivingMessage(source, sequence, ENFORCE_LEADER)) {
            context.buffer().putInt(LEADER_ID_OFFSET, leaderId);
            context.receive(PAYLOAD_LENGTH);
        }
    }

    @Override
    public Receiver.ReceivingContext receivingMessage(final int source, final long sequence, final int type) {
        return receivingContext.init(source, sequence, type);
    }

    private final class ReceivingContext implements Receiver.ReceivingContext {
        private final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        private AppendContext context;

        ReceivingContext init(final int source, final long sequence, final int type) {
            if (type != ENFORCE_LEADER) {
                throw new IllegalArgumentException("Unsupported type in enforce-leader input: " + type);
            }
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Receiving context not closed");
            }
            this.context = eventLogAppender.appending();
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightHeader.writeTo(
                    source, type, sequence, timeSource.currentTime(), Flags.COMMIT, (short)0, PAYLOAD_LENGTH,
                    context.buffer(), HEADER_OFFSET
            );
            return this;
        }

        private AppendContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Receiving context is closed");
        }

        @Override
        public MutableDirectBuffer buffer() {
            unclosedContext();
            return buffer;
        }

        @Override
        public void receive(final int length) {
            if (length != PAYLOAD_LENGTH) {
                throw new IllegalArgumentException("Enforce leader command must have length " + PAYLOAD_LENGTH);
            }
            if (!isValidLeaderId(ReplicationCommands.leaderId(buffer))) {
                abort();
                return;
            }
            completeEventPayload(buffer);
            buffer.unwrap();
            try (final AppendContext ac = unclosedContext()) {
                ac.commit(HEADER_LENGTH + length);
            } finally {
                context = null;
            }
        }

        @Override
        public void abort() {
            if (context != null) {
                buffer.unwrap();
                try {
                    context.abort();
                } finally {
                    context = null;
                }
            }
        }

        @Override
        public boolean isClosed() {
            return context == null;
        }

    }

    private void completeEventPayload(final MutableDirectBuffer buffer) {
        final int nextTerm = 1 + state.currentTerm();
        buffer.putByte(VERSION_OFFSET, VERSION);
        buffer.putByte(FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(TYPE_OFFSET, ENFORCE_LEADER);
        buffer.putInt(PAYLOAD_SIZE_OFFSET, 0);
        // already set: buffer.putInt(LEADER_ID_OFFSET, leaderId);
        buffer.putInt(TERM_OFFSET, nextTerm);
    }

    private boolean isValidLeaderId(final int leaderId) {
        if (leaderId == state.leaderId()) {
            logger.warn("Server {}: Ignoring enforce-leader input: enforced leader {} is already the current leader")
                    .replace(serverId).replace(leaderId).format();
            return false;
        }
        if (!serverIds.contains(leaderId)) {
            logger.warn("Server {}: Ignoring enforce-leader input: server ID {} is invalid")
                    .replace(serverId).replace(leaderId).format();
            return false;
        }
        return true;
    }
}
