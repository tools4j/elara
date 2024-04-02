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
package org.tools4j.elara.samples.replication;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.plugin.replication.Connection;
import org.tools4j.elara.plugin.replication.Connection.Handler;
import org.tools4j.elara.samples.network.Buffer;
import org.tools4j.elara.samples.network.ServerTopology;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.network.Buffer.CONSUMED_NOTHING;

public class ReceiveBufferPoller implements Connection.Poller {

    private final int serverId;
    private final IdMapping serverIds;
    private final Buffer[] receiverBuffers;
    private int roundRobin;
    private final MutableDirectBuffer buffer;

    public ReceiveBufferPoller(final int serverId,
                               final IdMapping serverIds,
                               final ServerTopology topology,
                               final int initialBufferCapacity) {
        this(serverId, serverIds, topology.receiveBuffers(serverIds.indexById(serverId)), initialBufferCapacity);
    }

    public ReceiveBufferPoller(final int serverId,
                               final IdMapping serverIds,
                               final Buffer[] receiverBuffers,
                               final int initialBufferCapacity) {
        if (serverIds.count() != receiverBuffers.length) {
            throw new IllegalArgumentException("there must be exactly 1 receive buffer per server, but it was " +
                    receiverBuffers.length + " buffers for " + serverIds.count() + " servers");
        }
        this.serverId = serverId;
        this.serverIds = requireNonNull(serverIds);
        this.receiverBuffers = requireNonNull(receiverBuffers);
        this.buffer = new ExpandableArrayBuffer(initialBufferCapacity);
    }

    @Override
    public int poll(final Handler handler) {
        requireNonNull(handler);
        final int n = receiverBuffers.length;

        int consumed = CONSUMED_NOTHING;
        int sender;
        for (sender = roundRobin; sender < n; sender++) {
            consumed = receiverBuffers[sender].consume(buffer, 0);
            if (consumed != CONSUMED_NOTHING) {
                break;
            }
        }
        if (consumed == CONSUMED_NOTHING) {
            for (sender = 0; sender < roundRobin; sender++) {
                consumed = receiverBuffers[sender].consume(buffer, 0);
                if (consumed != CONSUMED_NOTHING) {
                    break;
                }
            }
        }
        if (consumed != CONSUMED_NOTHING) {
            try {
                handler.onMessage(serverIds.idByIndex(sender), buffer, 0, consumed);
            } finally {
                buffer.setMemory(0, consumed, (byte)0);
            }
            roundRobin = sender + 1;
        }
        if (roundRobin >= n) {
            roundRobin = 0;
        }
        return consumed != CONSUMED_NOTHING ? 1 : 0;
    }
}
