/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.plugin.replication.Connection.Handler;
import org.tools4j.elara.plugin.replication.Connection.Poller;
import org.tools4j.nobark.loop.Step;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ConnectionHandler.RESEND_DELAY_NANOS;

public class ReplicationPluginStep implements Step {

    private final int serverId;
    private final int[] serverIds;
    private final ReplicationState.Volatile replicationState;
    private final EnforcedLeaderEventReceiver enforcedLeaderEventReceiver;
    private final Handler connectionHandler;
    private final EventSender eventSender;
    private final EnforceLeaderInput enforceLeaderInput;
    private final Connection.Poller[] connectionPollers;

    public ReplicationPluginStep(final Configuration configuration,
                                 final ReplicationState.Volatile replicationState,
                                 final EnforcedLeaderEventReceiver enforcedLeaderEventReceiver,
                                 final Handler connectionHandler,
                                 final EventSender eventSender) {
        this.serverId = configuration.serverId();
        this.serverIds = configuration.serverIds();
        this.replicationState = requireNonNull(replicationState);
        this.enforcedLeaderEventReceiver = requireNonNull(enforcedLeaderEventReceiver);
        this.connectionHandler = requireNonNull(connectionHandler);
        this.eventSender = requireNonNull(eventSender);
        this.enforceLeaderInput = configuration.enforceLeaderInput();
        this.connectionPollers = initPollers(configuration);
    }

    @Override
    public boolean perform() {
        boolean workDone = false;
        workDone |= pollEnforcedLeaderInput();
        workDone |= pollConnections();
        workDone |= updateFollowers();
        return workDone;
    }

    private boolean pollEnforcedLeaderInput() {
        return enforceLeaderInput.poll(enforcedLeaderEventReceiver) > 0;
    }

    private boolean pollConnections() {
        int polled = 0;
        for (final Poller poller : connectionPollers) {
            polled += poller.poll(connectionHandler);
        }
        return polled > 0;
    }

    private boolean updateFollowers() {
        if (isLeader()) {
            boolean workDone = false;
            final long eventLogSize = replicationState.eventLogSize();
            for (short server = 0; server < serverIds.length; server++) {
                final int followerId = serverIds[server];
                if (followerId != serverId) {
                    final long nextEventLogIndex = replicationState.nextEventLogIndex(followerId);
                    if (nextEventLogIndex < eventLogSize) {
                        if (eventSender.sendEvent(followerId, nextEventLogIndex)) {
                            replicationState.nextEventLogIndex(followerId, nextEventLogIndex + 1);
                        }
                        workDone = true;//we have still some work done if we move the poller forward or backward
                    } else {
                        final long confirmedEventLogIndex = replicationState.confirmedEventLogIndex(followerId);
                        if (confirmedEventLogIndex < eventLogSize) {
                            final long nanoTime = System.nanoTime();
                            final long nextTime = replicationState.nextNotBefore(followerId);
                            if (nextTime == 0 || nanoTime - nextTime >= 0) {
                                replicationState.nextEventLogIndex(followerId, confirmedEventLogIndex + 1);
                                replicationState.nextNotBefore(followerId, nanoTime + RESEND_DELAY_NANOS);
                                workDone = true;
                            }
                        }
                    }
                }
            }
            return workDone;
        }
        return false;
    }

    private boolean isLeader() {
        return serverId == replicationState.leaderId();
    }

    private Poller[] initPollers(final Configuration configuration) {
        final Set<Connection> connections = new LinkedHashSet<>();
        for (int i = 0; i < serverIds.length; i++) {
            connections.add(configuration.connection(serverIds[i]));
        }
        return connections.stream().map(Connection::poller).toArray(Poller[]::new);
    }
}
