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

import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.event.Event;

public class DefaultReplicationState implements ReplicationState.Mutable {

    private int currentTerm;
    private short leaderId = NULL_SERVER;
    private long eventLogSize;
    private final Long2LongHashMap nextEventLogIndexByServerId = new Long2LongHashMap(NULL_INDEX);

    @Override
    public int currentTerm() {
        return currentTerm;
    }

    @Override
    public Mutable currentTerm(final int term) {
        currentTerm = term;
        return this;
    }

    @Override
    public short leaderId() {
        return leaderId;
    }

    @Override
    public Mutable leaderId(final short leaderId) {
        this.leaderId = leaderId;
        return this;
    }

    @Override
    public long eventLogSize() {
        return eventLogSize;
    }

    @Override
    public Mutable eventApplied(final Event event) {
        eventLogSize++;
        return this;
    }

    @Override
    public long nextEventLogIndex(final int serverId) {
        return nextEventLogIndexByServerId.get(serverId);
    }

    @Override
    public Mutable nextEventLogIndex(final int serverId, final long index) {
        nextEventLogIndexByServerId.put(serverId, index);
        return this;
    }
}
