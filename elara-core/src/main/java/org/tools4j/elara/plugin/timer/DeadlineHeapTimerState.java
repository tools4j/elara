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
package org.tools4j.elara.plugin.timer;

import org.agrona.collections.Int2IntHashMap;
import org.agrona.collections.IntArrayList;
import org.tools4j.elara.plugin.timer.Timer.Style;
import org.tools4j.elara.plugin.timer.TimerStore.MutableTimerStore;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

/**
 * A timer state optimisation to efficiently find the timer with the next deadline.  Timers are ordered by deadline
 * in a heap data structure leading to<pre>
 *     - constant polling time when invoking {@link #indexOfNextDeadline()}
 *     - log(n) time to add or remove a timer
 *     - log(n) time to update the repetition of a periodic timer
 * </pre>
 * The data structure is very similar to the one used by {@link java.util.PriorityQueue}.
 */
public class DeadlineHeapTimerState implements MutableTimerState {
    private final Int2IntHashMap storeIndexToHeapIndex;
    private final IntArrayList deadlineHeapToStoreIndex;
    private final MutableTimerStore timerStore;

    public DeadlineHeapTimerState() {
        this(DirectTimerStore.DEFAULT_CAPACITY, new DirectTimerStore());
    }

    public DeadlineHeapTimerState(final int initialCapacity) {
        this(initialCapacity, new DirectTimerStore(initialCapacity));
    }

    public DeadlineHeapTimerState(final int initialCapacity, final MutableTimerStore timerStore) {
        this.storeIndexToHeapIndex = new Int2IntHashMap(2 * initialCapacity, DEFAULT_LOAD_FACTOR, -1);
        this.deadlineHeapToStoreIndex = new IntArrayList(initialCapacity, -1);
        this.timerStore = requireNonNull(timerStore);
    }

    @Override
    public int count() {
        return timerStore.count();
    }

    private int storeIndexToHeapIndex(final int index) {
        return storeIndexToHeapIndex.get(index);
    }

    private int heapIndexToStoreIndex(final int index) {
        return deadlineHeapToStoreIndex.getInt(index);
    }

    @Override
    public boolean hasTimer(final long timerId) {
        return timerStore.hasTimer(timerId);
    }

    @Override
    public int index(final long timerId) {
        final int storeIndex = timerStore.index(timerId);
        return storeIndex < 0 ? -1 : storeIndexToHeapIndex(storeIndex);
    }


    @Override
    public int indexOfNextDeadline() {
        return deadlineHeapToStoreIndex.isEmpty() ? -1 : 0;
    }

    @Override
    public long timerId(final int index) {
        return timerStore.timerId(heapIndexToStoreIndex(index));
    }

    @Override
    public Style style(final int index) {
        return timerStore.style(heapIndexToStoreIndex(index));
    }

    @Override
    public int repetition(final int index) {
        return timerStore.repetition(heapIndexToStoreIndex(index));
    }

    @Override
    public long startTime(final int index) {
        return timerStore.startTime(heapIndexToStoreIndex(index));
    }

    @Override
    public long timeout(final int index) {
        return timerStore.timeout(heapIndexToStoreIndex(index));
    }

    @Override
    public int timerType(final int index) {
        return timerStore.timerType(heapIndexToStoreIndex(index));
    }

    @Override
    public long contextId(final int index) {
        return timerStore.contextId(heapIndexToStoreIndex(index));
    }

    @Override
    public long deadline(final int index) {
        return timerStore.deadline(heapIndexToStoreIndex(index));
    }

    @Override
    public boolean add(final long timerId, final Style style, final int repetition, final long startTime, final long timeout, final int timerType, final long contextId) {
        final int storeIndex = timerStore.count();//NOTE: we know the store adds a new entry at the end
        if (timerStore.add(timerId, style, repetition, startTime, timeout, timerType, contextId)) {
            add(storeIndex);
            return true;
        }
        return false;
    }

    private void add(final int storeIndex) {
        final int heapIndex = deadlineHeapToStoreIndex.size();
        if (heapIndex == 0) {
            deadlineHeapToStoreIndex.addInt(storeIndex);
            storeIndexToHeapIndex.put(storeIndex, heapIndex);
        } else {
            deadlineHeapToStoreIndex.addInt(-1);
            siftUp(heapIndex, storeIndex);
        }
    }

    private int siftUp(final int heapIndex, final int storeIndex) {
        //see PriorityQueue.siftUp(..)
        final long deadline = timerStore.deadline(storeIndex);
        int k = heapIndex;
        while (k > 0) {
            final int parent = (k - 1) >>> 1;
            final int p = deadlineHeapToStoreIndex.getInt(parent);
            if (deadline >= timerStore.deadline(p)) {
                break;
            }
            set(k, p);
            k = parent;
        }
        set(k, storeIndex);
        return k;
    }

    private int siftDown(final int heapIndex, final int storeIndex) {
        //see PriorityQueue.siftDown(..)
        final long deadline = timerStore.deadline(storeIndex);
        final int size = deadlineHeapToStoreIndex.size();
        int k = heapIndex;

        int half = size >>> 1;
        while (k < half) {
            int child = (k << 1) + 1;
            final int right = child + 1;
            int c = deadlineHeapToStoreIndex.getInt(child);
            if (right < size) {
                final int r = deadlineHeapToStoreIndex.getInt(right);
                if (timerStore.deadline(c) > timerStore.deadline(r)) {
                    child = right;
                    c = r;
                }
            }
            if (deadline <= timerStore.deadline(c)) {
                break;
            }
            set(k, c);
            k = child;
        }
        set(k, storeIndex);
        return k;
    }

    private void set(final int heapIndex, final int storeIndex) {
        storeIndexToHeapIndex.put(storeIndex, heapIndex);
        deadlineHeapToStoreIndex.setInt(heapIndex, storeIndex);
    }

    @Override
    public void remove(final int index) {
        final int storeIndex = deadlineHeapToStoreIndex.setInt(index, -1);
        storeIndexToHeapIndex.remove(storeIndex);
        final int s = deadlineHeapToStoreIndex.size() - 1;
        final int movedStoreIndex = deadlineHeapToStoreIndex.removeAt(s);
        if (s != index) {
            if (index == siftDown(index, movedStoreIndex)) {
                siftUp(index, movedStoreIndex);
            }
        }
        //NOTE: we know that removing timerStore[storeIndex] from the store will swap the last element into its place
        timerStore.remove(storeIndex);
        final int swappedStoreIndex = timerStore.count();
        if (storeIndex != swappedStoreIndex) {
            final int heapIndex = storeIndexToHeapIndex.remove(swappedStoreIndex);
            storeIndexToHeapIndex.put(storeIndex, heapIndex);
            deadlineHeapToStoreIndex.setInt(heapIndex, storeIndex);
        }
    }

    @Override
    public void removeAll() {
        storeIndexToHeapIndex.clear();
        deadlineHeapToStoreIndex.clear();
        timerStore.removeAll();
    }

    @Override
    public void updateRepetitionById(final long timerId, final int repetition) {
        final int storeIndex = timerStore.index(timerId);
        if (storeIndex < 0) {
            return;
        }
        final int prevRepetition = timerStore.repetition(storeIndex);
        if (prevRepetition == repetition) {
            return;
        }
        timerStore.updateRepetition(storeIndex, repetition);
        final int heapIndex = storeIndexToHeapIndex(storeIndex);
        if (repetition > prevRepetition) {
            siftDown(heapIndex, storeIndex);
        } else {
            siftUp(heapIndex, storeIndex);
        }
    }

    @Override
    public String toString() {
        if (count() == 0) {
            return "DeadlineHeapTimerState{}";
        }
        return "DeadlineHeapTimerState{next=" + timerId(indexOfNextDeadline()) + ", timers=" + timerStore + "}";
    }
}
