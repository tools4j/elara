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
import org.agrona.collections.LongArrayList;

public class SimpleTimerState implements TimerState.Mutable {

    private final IntArrayList types = new IntArrayList();
    private final LongArrayList ids = new LongArrayList();
    private final LongArrayList times = new LongArrayList();
    private final LongArrayList timeouts = new LongArrayList();

    @Override
    public int count() {
        return ids.size();
    }

    @Override
    public int type(final int index) {
        return types.get(index);
    }

    @Override
    public long id(final int index) {
        return ids.get(index);
    }

    @Override
    public long time(final int index) {
        return times.get(index);
    }

    @Override
    public long timeout(final int index) {
        return timeouts.get(index);
    }

    public int indexById(final long id) {
        return ids.indexOf(id);
    }

    @Override
    public boolean remove(final long id) {
        final int index = indexById(id);
        if (index >= 0) {
            removeByIndex(index);
            return true;
        }
        return false;
    }

    public void removeByIndex(final int index) {
        ids.fastUnorderedRemove(index);
        types.fastUnorderedRemove(index);
        times.fastUnorderedRemove(index);
        timeouts.fastUnorderedRemove(index);
    }

    @Override
    public boolean add(final int type, final long id, final long time, final long timeout) {
        if (ids.containsLong(id)) {
            return false;
        }
        types.add(type);
        ids.add(id);
        times.add(time);
        timeouts.add(timeout);
        return true;
    }
}
