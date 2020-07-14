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
    private int leaderId = NULL_SERVER;
    private long eventLogSize;
    private final Long2LongHashMap nextEventLogIndexByServerId = new Long2LongHashMap(0);
    private final Long2LongHashMap nextNotBefore = new Long2LongHashMap(0);

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
    public int leaderId() {
        return leaderId;
    }

    @Override
    public Mutable leaderId(final int leaderId) {
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
    public Volatile nextEventLogIndex(final int serverId, final long index) {
        putOrRemove(nextEventLogIndexByServerId, serverId, index);
        return this;
    }

    @Override
    public long nextNotBefore(final int serverId) {
        return nextNotBefore.get(serverId);
    }

    @Override
    public Volatile nextNotBefore(final int serverId, final long nanos) {
        putOrRemove(nextNotBefore, serverId, nanos);
        return this;
    }

    private static void putOrRemove(final Long2LongHashMap map, final long key, final long value) {
        if (value == map.missingValue()) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    @Override
    public String toString() {
        return "DefaultReplicationState{" +
                "currentTerm=" + currentTerm +
                ", leaderId=" + leaderId +
                ", eventLogSize=" + eventLogSize +
                ", nextEventLogIndexByServerId=" + nextEventLogIndexByServerId +
                ", nextNotBefore=" + nextNotBefore +
                '}';
    }
}