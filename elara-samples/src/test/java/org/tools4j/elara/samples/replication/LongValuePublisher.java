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
package org.tools4j.elara.samples.replication;

import org.agrona.DirectBuffer;
import org.tools4j.elara.plugin.replication.Connection;
import org.tools4j.elara.samples.network.ServerTopology;

import static java.util.Objects.requireNonNull;

public class LongValuePublisher implements Connection.Publisher {

    private final int senderServerId;
    private final IdMapping serverIds;
    private final ServerTopology topology;

    public LongValuePublisher(final int senderServerId, final IdMapping serverIds, final ServerTopology topology) {
        if (serverIds.count() != topology.senders()) {
            throw new IllegalArgumentException("Servers " + serverIds.count() + " must equal number of senders " +
                    topology.senders());
        }
        if (serverIds.count() != topology.receivers()) {
            throw new IllegalArgumentException("Servers " + serverIds.count() + " must equal number of receivers " +
                    topology.receivers());
        }
        this.senderServerId = senderServerId;
        this.serverIds = requireNonNull(serverIds);
        this.topology = requireNonNull(topology);
    }

    @Override
    public boolean publish(final int targetServerId, final DirectBuffer buffer, final int offset, final int length) {
        final int sender = serverIds.indexById(senderServerId);
        final int receiver = serverIds.indexById(targetServerId);
        return topology.transmit(sender, receiver, buffer.getLong(0));
    }
}
