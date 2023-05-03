/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.timer.Timer.Style;

public interface TimerStore {
    int count();
    int index(long timerId);
    long timerId(int index);
    Style style(int index);
    int repetition(int index);
    long startTime(int index);
    long timeout(int index);
    int timerType(int index);
    long contextId(int index);
    long deadline(int index);

    default boolean hasTimer(final long timerId) {
        return index(timerId) >= 0;
    }

    interface MutableTimerStore extends TimerStore {
        default boolean add(final Event event, final Timer timer) {
            return add(timer.timerId(), timer.style(), timer.repetition(), event.eventTime(), timer.timeout(),
                    timer.timerType(), timer.contextId());
        }
        boolean add(long timerId, Style style, int repetition, long startTime, long timeout, int timerType, long contextId);

        void remove(int index);
        void updateRepetition(int index, int repetition);
        default boolean updateRepetitionById(final long timerId, final int repetition) {
            final int index = index(timerId);
            if (index >= 0) {
                updateRepetition(index, repetition);
                return true;
            }
            return false;
        }

        default boolean removeById(final long timerId) {
            final int index = index(timerId);
            if (index >= 0) {
                remove(index);
                return true;
            }
            return false;
        }
    }
}
