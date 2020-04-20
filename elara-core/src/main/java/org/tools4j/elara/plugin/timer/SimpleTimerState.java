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
package org.tools4j.elara.plugin.timer;

import org.agrona.collections.IntArrayList;
import org.agrona.collections.Long2LongHashMap;
import org.agrona.collections.LongArrayList;

import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

public class SimpleTimerState implements TimerState.Mutable {

    public static final int DEFAULT_INITIAL_CAPACITY = 64;

    private final Long2LongHashMap idToIndex;
    private final LongArrayList ids;
    private final IntArrayList types;
    private final LongArrayList times;
    private final LongArrayList timeouts;

    public SimpleTimerState() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public SimpleTimerState(final int initialCapacity) {
        this.idToIndex = new Long2LongHashMap(2 * initialCapacity, DEFAULT_LOAD_FACTOR, -1);
        this.ids = new LongArrayList(initialCapacity, 0);
        this.types = new IntArrayList(initialCapacity, 0);
        this.times = new LongArrayList(initialCapacity, 0);
        this.timeouts = new LongArrayList(initialCapacity, 0);
    }

    @Override
    public int count() {
        return ids.size();
    }

    @Override
    public boolean hasTimer(final long id) {
        return idToIndex.containsKey(id);
    }

    @Override
    public long id(final int index) {
        return ids.getLong(index);
    }

    @Override
    public int type(final int index) {
        return types.getInt(index);
    }

    @Override
    public long time(final int index) {
        return times.getLong(index);
    }

    @Override
    public long timeout(final int index) {
        return timeouts.getLong(index);
    }

    @Override
    public int indexById(final long id) {
        return (int)idToIndex.get(id);
    }

    @Override
    public void remove(final int index) {
        final long id = ids.fastUnorderedRemove(index);
        idToIndex.remove(id);
        types.fastUnorderedRemove(index);
        times.fastUnorderedRemove(index);
        timeouts.fastUnorderedRemove(index);
        if (index < ids.size()) {
            idToIndex.put(ids.get(index), index);
        }
    }

    @Override
    public boolean add(final long id, final int type, final long time, final long timeout) {
        if (!hasTimer(id) && timeout >= 0) {
            idToIndex.put(id, ids.size());
            ids.addLong(id);
            types.addInt(type);
            times.addLong(time);
            timeouts.addLong(timeout);
            return true;
        }
        return false;
    }
}
