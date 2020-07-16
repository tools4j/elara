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

import org.agrona.collections.Long2LongHashMap;

import java.util.ArrayList;
import java.util.List;

import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

/**
 * A timer state optimisation to efficiently find the next triggering timer.  Orders timer IDs by deadline
 * in a heap data structure leading to<pre>
 *     - constant polling time via indexOfNextDeadline()
 *     - log(n) time to add or remove a timer
 *     - log(n) time to change the repetition of a timer
 * </pre>
 * The data structure is very similar to that used by {@link java.util.PriorityQueue}.
 */
public class DeadlineHeapTimerState implements TimerState.Mutable {

    public static final int DEFAULT_INITIAL_CAPACITY = 64;

    private final Long2LongHashMap idToIndex;
    private final List<Timer> timerHeapByDeadline;
    private final List<Timer> unused;

    private static final class Timer {
        long id;
        int type;
        int repetition;
        long time;
        long timeout;
        final int nextRepetition() {
            return Static.nextRepetition(repetition);
        }
        final long deadline() {
            return Static.deadline(time, timeout, nextRepetition());
        }
        final Timer init(final long id, final int type, final int repetition, final long time, final long timeout) {
            this.id = id;
            this.type = type;
            this.repetition = repetition;
            this.time = time;
            this.timeout = timeout;
            return this;
        }
        final Timer reset() {
            return init(0, 0, 0, 0, 0);
        }
    }

    public DeadlineHeapTimerState() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    public DeadlineHeapTimerState(final int initialCapacity) {
        this.idToIndex = new Long2LongHashMap(2 * initialCapacity, DEFAULT_LOAD_FACTOR, -1);
        this.timerHeapByDeadline = new ArrayList<>(initialCapacity);
        this.unused = new ArrayList<>(initialCapacity);
        for (int i = 0; i < initialCapacity; i++) {
            unused.add(new Timer());
        }
    }

    @Override
    public int count() {
        return timerHeapByDeadline.size();
    }

    @Override
    public int indexById(final long id) {
        return (int)idToIndex.get(id);
    }

    @Override
    public long id(final int index) {
        return timerHeapByDeadline.get(index).id;
    }

    @Override
    public int type(final int index) {
        return timerHeapByDeadline.get(index).type;
    }

    @Override
    public int repetition(final int index) {
        return timerHeapByDeadline.get(index).repetition;
    }

    @Override
    public long time(final int index) {
        return timerHeapByDeadline.get(index).time;
    }

    @Override
    public long timeout(final int index) {
        return timerHeapByDeadline.get(index).timeout;
    }

    @Override
    public long deadline(final int index) {
        return timerHeapByDeadline.get(index).deadline();
    }

    @Override
    public int nextRepetition(final int index) {
        return timerHeapByDeadline.get(index).nextRepetition();
    }

    @Override
    public boolean hasTimer(final long id) {
        return idToIndex.containsKey(id);
    }

    @Override
    public int indexOfNextDeadline() {
        return timerHeapByDeadline.isEmpty() ? -1 : 0;
    }

    @Override
    public boolean add(final long id, final int type, final int repetition, final long time, final long timeout) {
        if (!hasTimer(id)) {
            add(acquire().init(id, type, repetition, time, timeout));
            return true;
        }
        return false;
    }

    private void add(final Timer timer) {
        final int index = timerHeapByDeadline.size();
        if (index == 0) {
            timerHeapByDeadline.add(timer);
            idToIndex.put(timer.id, index);
        } else {
            timerHeapByDeadline.add(null);
            siftUp(index, timer);
        }
    }

    private void siftUp(final int index, final Timer timer) {
        //see PriorityQueue.siftUp(..)
        final long deadline = timer.deadline();
        int k = index;
        while (k > 0) {
            final int parent = (k - 1) >>> 1;
            final Timer p = timerHeapByDeadline.get(parent);
            if (deadline >= p.deadline()) {
                break;
            }
            set(k, p);
            k = parent;
        }
        set(k, timer);
    }

    private void siftDown(final int index, final Timer timer) {
        //see PriorityQueue.siftDown(..)
        final long deadline = timer.deadline();
        final int size = timerHeapByDeadline.size();
        int k = index;

        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            int right = child + 1;
            Timer c = timerHeapByDeadline.get(child);
            if (right < size) {
                final Timer r = timerHeapByDeadline.get(right);
                if (c.deadline() > r.deadline()) {
                    child = right;
                    c = r;
                }
            }
            if (deadline <= c.deadline()) {
                break;
            }
            set(k, c);
            k = child;
        }
        set(k, timer);
    }

    private Timer set(final int index, final Timer timer) {
        idToIndex.put(timer.id, index);
        return timerHeapByDeadline.set(index, timer);
    }

    @Override
    public void remove(final int index) {
        final Timer timer = timerHeapByDeadline.set(index, null);
        idToIndex.remove(timer.id);
        final int s = timerHeapByDeadline.size() - 1;
        final Timer moved = timerHeapByDeadline.remove(s);
        if (s != index) {
            siftDown(index, moved);
        }
        release(timer);
    }

    @Override
    public void repetition(final int index, final int repetition) {
        final Timer timer = timerHeapByDeadline.get(index);
        final int prevRepetition = timer.repetition;
        if (timer.repetition == repetition) {
            return;
        }
        timer.repetition = repetition;
        if (repetition > prevRepetition) {
            siftDown(index, timer);
        } else {
            siftUp(index, timer);
        }
    }

    private Timer acquire() {
        final int last = unused.size() - 1;
        return last >= 0 ? unused.remove(last) : new Timer();
    }

    private void release(final Timer timer) {
        unused.add(timer.reset());
    }

    @Override
    public String toString() {
        if (count() == 0) {
            return "DeadlineHeapTimerState{}";
        }
        return "DeadlineHeapTimerState{" +
                "next=" + id(indexOfNextDeadline()) +
                ", ids=" + idToIndex.keySet() + "}";
    }
}
