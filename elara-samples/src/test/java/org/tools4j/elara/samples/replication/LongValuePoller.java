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

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.plugin.replication.Connection;
import org.tools4j.elara.plugin.replication.Connection.Handler;
import org.tools4j.elara.samples.network.Buffer;
import org.tools4j.elara.samples.network.ServerTopology;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.network.Buffer.NULL_VALUE;

public class LongValuePoller implements Connection.Poller {

    private final IdMapping serverIds;
    private final Buffer[] receiverBuffers;
    private int roundRobin;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[Long.BYTES]);

    public LongValuePoller(final int serverId, final IdMapping serverIds, final ServerTopology topology) {
        this(serverIds, topology.receiveBuffers(serverIds.indexById(serverId)));
    }

    public LongValuePoller(final IdMapping serverIds, final Buffer[] receiverBuffers) {
        if (serverIds.count() != receiverBuffers.length) {
            throw new IllegalArgumentException("there must be exactly 1 receive buffer per server, but it was " +
                    receiverBuffers.length + " buffers for " + serverIds.count() + " servers");
        }
        this.serverIds = requireNonNull(serverIds);
        this.receiverBuffers = requireNonNull(receiverBuffers);
    }

    @Override
    public int poll(final Handler handler) {
        requireNonNull(handler);
        final int n = receiverBuffers.length;

        long value = NULL_VALUE;
        int sender;
        for (sender = roundRobin; sender < n && value == NULL_VALUE; sender++) {
            value = receiverBuffers[sender].consume();
        }
        if (value == NULL_VALUE) {
            for (sender = 0; sender < roundRobin && value == NULL_VALUE; sender++) {
                value = receiverBuffers[sender].consume();
            }
        }
        if (value != NULL_VALUE) {
            roundRobin = sender + 1 >= n ? 0 : sender + 1;
            buffer.putLong(0, value);
            try {
                handler.onMessage(serverIds.idByIndex(sender), buffer, 0, Long.BYTES);
            } finally {
                buffer.putLong(0, 0);
            }
            return 1;
        }
        return 0;
    }
}
