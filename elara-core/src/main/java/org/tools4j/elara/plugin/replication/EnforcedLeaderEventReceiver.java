/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger.Factory;
import org.tools4j.elara.plugin.replication.EnforceLeaderInput.EnforceLeaderReceiver;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.LEADER_CONFIRMED;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.LEADER_ENFORCED;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.LEADER_REJECTED;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

final class EnforcedLeaderEventReceiver implements EnforceLeaderReceiver {

    private final ElaraLogger logger;
    private final TimeSource timeSource;
    private final int serverId;
    private final IntHashSet serverIds;
    private final ReplicationState state;
    private final Appender eventStoreAppender;

    EnforcedLeaderEventReceiver(final Factory loggerFactory,
                                final TimeSource timeSource,
                                final Configuration configuration,
                                final ReplicationState state,
                                final Appender eventStoreAppender) {
        this.logger = ElaraLogger.create(loggerFactory, getClass());
        this.timeSource = requireNonNull(timeSource);
        this.serverIds = new IntHashSet(NULL_SERVER);
        this.state = requireNonNull(state);
        this.eventStoreAppender = requireNonNull(eventStoreAppender);
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
    public int term() {
        return state.term();
    }

    @Override
    public int leaderId() {
        return state.leaderId();
    }

    @Override
    public void enforceLeader(final int sourceId, final long sourceSeq, final int leaderId) {
        final long time = timeSource.currentTime();
        final int currentTerm = state.term();
        if (leaderId == state.leaderId()) {
            logger.info("Server {} processing enforce-leader request: leader confirmed since attempted enforced leader {} is already leader")
                    .replace(serverId).replace(leaderId).format();
            try (final AppendingContext context = eventStoreAppender.appending()) {
                FlyweightHeader.writeTo(
                        sourceId, LEADER_CONFIRMED, sourceSeq, time, Flags.COMMIT, (short)0, PAYLOAD_LENGTH,
                        context.buffer(), HEADER_OFFSET
                );
                ReplicationEvents.leaderConfirmed(context.buffer(), PAYLOAD_OFFSET, currentTerm, leaderId);
                context.commit(HEADER_LENGTH + PAYLOAD_LENGTH);
            }
            return;
        }
        if (!serverIds.contains(leaderId)) {
            logger.info("Server {} processing enforce-leader request: rejected since leader ID {} is invalid")
                    .replace(serverId).replace(leaderId).format();
            try (final AppendingContext context = eventStoreAppender.appending()) {
                FlyweightHeader.writeTo(
                        sourceId, LEADER_REJECTED, sourceSeq, time, Flags.COMMIT, (short)0, PAYLOAD_LENGTH,
                        context.buffer(), HEADER_OFFSET
                );
                ReplicationEvents.leaderRejected(context.buffer(), PAYLOAD_OFFSET, currentTerm, leaderId);
                context.commit(HEADER_LENGTH + PAYLOAD_LENGTH);
            }
            return;
        }
        final int nextTerm = currentTerm + 1;
        logger.info("Server {} processing enforce-leader request: enforcing leader {} to replace current leader {} for next term {}")
                .replace(serverId).replace(leaderId).replace(state.leaderId()).replace(nextTerm).format();
        try (final AppendingContext context = eventStoreAppender.appending()) {
            FlyweightHeader.writeTo(
                    sourceId, LEADER_ENFORCED, sourceSeq, timeSource.currentTime(), Flags.COMMIT, (short)0, PAYLOAD_LENGTH,
                    context.buffer(), HEADER_OFFSET
            );
            ReplicationEvents.leaderEnforced(context.buffer(), PAYLOAD_OFFSET, nextTerm, leaderId);
            context.commit(HEADER_LENGTH + PAYLOAD_LENGTH);
        }
    }
}
