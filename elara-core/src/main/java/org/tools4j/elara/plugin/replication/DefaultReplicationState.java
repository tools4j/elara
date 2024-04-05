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

import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.Long2LongHashMap.ValueIterator;
import org.agrona.collections.LongArrayList;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.time.TimeSource;

public class DefaultReplicationState implements ReplicationState.Mutable {

    private int term;
    private int leaderId = NULL_SERVER;
    private long lastAppliedEventSequence = BaseState.NIL_SEQUENCE;
    private long lastAppliedEventTime = TimeSource.MIN_VALUE;
    private long eventStoreSize;
    private final Long2LongHashMap nextEventStoreIndexByServerId = new Long2LongHashMap(0);
    private final Long2LongHashMap confirmedEventStoreIndexByServerId = new Long2LongHashMap(0);
    private final Long2LongHashMap nextNotBefore = new Long2LongHashMap(0);
    private final LongArrayList temp = new LongArrayList();

    @Override
    public int term() {
        return term;
    }

    @Override
    public Mutable term(final int term) {
        this.term = term;
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
    public long lastAppliedEventSequence() {
        return lastAppliedEventSequence;
    }

    @Override
    public long lastAppliedEventTime() {
        return lastAppliedEventTime;
    }

    @Override
    public long eventStoreSize() {
        return eventStoreSize;
    }

    @Override
    public Mutable eventApplied(final Event event) {
        lastAppliedEventSequence = event.eventSequence();
        lastAppliedEventTime = event.eventTime();
        eventStoreSize++;
        return this;
    }

    @Override
    public long nextEventStoreIndex(final int serverId) {
        return nextEventStoreIndexByServerId.get(serverId);
    }

    @Override
    public Volatile nextEventStoreIndex(final int serverId, final long index) {
        putOrRemove(nextEventStoreIndexByServerId, serverId, index);
        return this;
    }

    @Override
    public long confirmedEventStoreIndex(final int serverId) {
        return confirmedEventStoreIndexByServerId.get(serverId);
    }

    @Override
    public Volatile confirmedEventStoreIndex(final int serverId, final long index) {
        putOrRemove(confirmedEventStoreIndexByServerId, serverId, index);
        return this;
    }

    @Override
    public long committedEventStoreIndex(final int serverCount) {
        final int majority = (serverCount / 2) + 1;
        if (confirmedEventStoreIndexByServerId.size() < majority) {
            return -1;
        }
        temp.clear();
        final ValueIterator it = confirmedEventStoreIndexByServerId.values().iterator();
        while (it.hasNext()) {
            final long value = it.nextValue();
            int index = temp.size();
            temp.addLong(value);
            while (index > 0 && value > temp.get(index - 1)) {
                temp.set(index, temp.get(index - 1));
            }
            temp.set(index, value);
        }
        return temp.get(majority - 1);
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
        return "DefaultReplicationState:" +
                ":term=" + term +
                "|leader-id=" + leaderId +
                "|last-applied-evt-seq=" + lastAppliedEventSequence +
                "|last-applied-evt-time=" + lastAppliedEventTime +
                "|evt-store-size=" + eventStoreSize +
                "|next-evt-store-index-by-server-id=" + nextEventStoreIndexByServerId +
                "|confirmed-evt-store-index-by-server-id=" + confirmedEventStoreIndexByServerId +
                "|next-not-before=" + nextNotBefore +
                "|temp=" + temp;
    }
}
