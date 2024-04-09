/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.replication.ReplicationEvents.replicationEventName;

public class ReplicationEventApplier implements EventApplier {

    private final ElaraLogger logger;
    private final ReplicationConfig configuration;
    private final ReplicationState.Mutable replicationState;

    public ReplicationEventApplier(final Logger.Factory loggerFactory,
                                   final ReplicationConfig configuration,
                                   final ReplicationState.Mutable replicationState) {
        this.logger = ElaraLogger.create(loggerFactory, getClass());
        this.configuration = requireNonNull(configuration);
        this.replicationState = requireNonNull(replicationState);
    }

    @Override
    public void onEvent(final Event event) {
        switch (event.payloadType()) {
            case ReplicationEvents.LEADER_ELECTED://same for both
            case ReplicationEvents.LEADER_ENFORCED:
                updateLeader(event);
                break;
            case ReplicationEvents.LEADER_CONFIRMED://same for both
            case ReplicationEvents.LEADER_REJECTED:
                //no op
                break;
        }
        replicationState.eventApplied(event);
    }

    private void updateLeader(final Event event) {
        final int leaderId = ReplicationEvents.leaderId(event);
        final int term = ReplicationEvents.term(event);
        updateLeader(replicationEventName(event), term, leaderId);
    }

    private void updateLeader(final String eventName, final int term, final int leaderId) {
        final int serverId = configuration.serverId();
        replicationState.term(term).leaderId(leaderId);
        logger.info("Server {} applied {}: Updated leader to {} for term {}")
                .replace(serverId).replace(eventName).replace(leaderId).replace(term).format();
    }
}
