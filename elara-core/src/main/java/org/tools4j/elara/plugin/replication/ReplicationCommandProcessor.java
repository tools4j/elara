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

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.leaderElected;

public class ReplicationCommandProcessor implements CommandProcessor {

    private final ReplicationState replicationState;

    public ReplicationCommandProcessor(final ReplicationState replicationState) {
        this.replicationState = requireNonNull(replicationState);
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        if (command.type() == ReplicationCommands.PROPOSE_LEADER) {
            final int candidateId = ReplicationCommands.candidateId(command);
            if (candidateId != replicationState.leaderId()) {
                final int nextTerm = replicationState.currentTerm() + 1;
                try (final RoutingContext context = router.routingEvent(ReplicationEvents.LEADER_ELECTED)) {
                    final int length = leaderElected(context.buffer(), 0, nextTerm, candidateId);
                    context.route(length);
                }
            }
        }
    }
}
