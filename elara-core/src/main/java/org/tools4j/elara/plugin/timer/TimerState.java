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

import org.tools4j.elara.time.TimeSource;

public interface TimerState {
    /** Repetition value for a simple timer without repetitions */
    int REPETITION_SINGLE = -1;

    int count();
    int indexById(long id);
    long id(int index);
    int type(int index);
    int repetition(int index);
    long time(int index);
    long timeout(int index);

    default long deadline(final int index) {
        return Static.deadline(time(index), timeout(index), nextRepetition(index));
    }

    default int nextRepetition(final int index) {
        return Static.nextRepetition(repetition(index));
    }

    default boolean hasTimer(final long id) {
        return indexById(id) >= 0;
    }

    default int indexOfNextDeadline() {
        final int count = count();
        int index = -1;
        long minDeadline = TimeSource.END_OF_TIME;
        for (int i = 0; i < count; i++) {
            final long deadline = deadline(i);
            if (deadline < minDeadline) {
                index = i;
                minDeadline = deadline;
            }
        }
        return index;
    }

    interface Mutable extends TimerState {
        boolean add(long id, int type, int repetition, long time, long timeout);
        void remove(int index);
        void repetition(int index, int repetition);

        default boolean removeById(final long id) {
            final int index = indexById(id);
            if (index >= 0) {
                remove(index);
                return true;
            }
            return false;
        }
    }

    interface Static {
        static long deadline(final long time, final long timeout, final int nextRepetition) {
            return time + timeout * nextRepetition;
        }

        static int nextRepetition(final int repetition) {
            return repetition == REPETITION_SINGLE ? 1 : repetition + 1;
        }
    }
}
