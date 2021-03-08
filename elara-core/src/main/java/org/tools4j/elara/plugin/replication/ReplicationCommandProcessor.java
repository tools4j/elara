/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationCommands.replicationCommandName;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.leaderConfirmed;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.leaderElected;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.leaderRejected;
import static org.tools4j.elara.plugin.replication.ReplicationState.NULL_SERVER;

public class ReplicationCommandProcessor implements CommandProcessor {

    private final ElaraLogger logger;
    private final Configuration configuration;
    private final IntHashSet serverIds;
    private final ReplicationState replicationState;

    public ReplicationCommandProcessor(final Logger.Factory loggerFactory,
                                       final Configuration configuration,
                                       final ReplicationState replicationState) {
        this.logger = ElaraLogger.create(loggerFactory, getClass());
        this.configuration = requireNonNull(configuration);
        this.serverIds = new IntHashSet(NULL_SERVER);
        this.replicationState = requireNonNull(replicationState);
        for (final int serverId : configuration.serverIds()) {
            serverIds.add(serverId);
        }
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        if (command.type() == ReplicationCommands.PROPOSE_LEADER) {
            final String commandName = replicationCommandName(command);
            final int serverId = configuration.serverId();
            final int candidateId = ReplicationCommands.candidateId(command);
            final int leaderId = replicationState.leaderId();
            final int currentTerm = replicationState.term();
            if (candidateId == leaderId) {
                logger.info("Server {} processing {}: leader confirmed since proposed candidate {} is already leader")
                        .replace(serverId).replace(commandName).replace(candidateId).format();
                try (final RoutingContext context = router.routingEvent(ReplicationEvents.LEADER_CONFIRMED)) {
                    final int length = leaderConfirmed(context.buffer(), 0, currentTerm, candidateId);
                    context.route(length);
                }
                return;
            }
            if (!serverIds.contains(candidateId)) {
                logger.info("Server {} processing {}: rejected since candidate ID {} is invalid")
                        .replace(serverId).replace(commandName).replace(candidateId).format();
                try (final RoutingContext context = router.routingEvent(ReplicationEvents.LEADER_REJECTED)) {
                    final int length = leaderRejected(context.buffer(), 0, currentTerm, candidateId);
                    context.route(length);
                }
                return;
            }
            final int nextTerm = currentTerm + 1;
            logger.info("Server {} processing {}: electing candidate {} to replace current leader {} for next term {}")
                    .replace(serverId).replace(commandName).replace(candidateId)
                    .replace(leaderId).replace(nextTerm).format();
            try (final RoutingContext context = router.routingEvent(ReplicationEvents.LEADER_ELECTED)) {
                final int length = leaderElected(context.buffer(), 0, nextTerm, candidateId);
                context.route(length);
            }
        }
    }
}
