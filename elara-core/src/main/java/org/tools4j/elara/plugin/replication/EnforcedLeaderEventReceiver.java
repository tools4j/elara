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

import org.agrona.collections.IntHashSet;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightHeader;
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
import static org.tools4j.elara.plugin.replication.ReplicationEvents.LEADER_ENFORCED;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

final class EnforcedLeaderEventReceiver implements EnforceLeaderReceiver {

    private final ElaraLogger logger;
    private final TimeSource timeSource;
    private final int serverId;
    private final IntHashSet serverIds;
    private final ReplicationState state;
    private final Appender eventLogAppender;

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
        final int nextTerm = 1 + state.currentTerm();
        try (final AppendContext context = eventLogAppender.appending()) {
            FlyweightHeader.writeTo(
                    source, LEADER_ENFORCED, sequence, timeSource.currentTime(), Flags.COMMIT, (short)0, PAYLOAD_LENGTH,
                    context.buffer(), HEADER_OFFSET
            );
            ReplicationEvents.leaderEnforced(context.buffer(), PAYLOAD_OFFSET, nextTerm, leaderId);
            context.commit(HEADER_LENGTH + PAYLOAD_LENGTH);
        }
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
