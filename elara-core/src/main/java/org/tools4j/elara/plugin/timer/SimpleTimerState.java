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

import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.plugin.timer.Timer.Style;
import org.tools4j.elara.plugin.timer.TimerStore.MutableTimerStore;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

/**
 * A simple implementation of {@link TimerState} that iterates through all timers in order to find the one with the next
 * deadline.
 * <p>
 * Note that this timer state implementation is not recommended for larger numbers of concurrent timers as it takes
 * O(n) time to find the timer with the next deadline!
 */
public class SimpleTimerState implements MutableTimerState {

    private final MutableTimerStore timers;

    public SimpleTimerState() {
        this(new DirectTimerStore());
    }

    public SimpleTimerState(final MutableTimerStore timers) {
        this.timers = requireNonNull(timers);
    }

    @Override
    public int count() {
        return timers.count();
    }

    @Override
    public int index(final long timerId) {
        return timers.index(timerId);
    }


    @Override
    public boolean hasTimer(final long timerId) {
        return timers.hasTimer(timerId);
    }

    @Override
    public long timerId(final int index) {
        return timers.timerId(index);
    }

    @Override
    public Style style(final int index) {
        return timers.style(index);
    }

    @Override
    public int repetition(final int index) {
        return timers.repetition(index);
    }

    @Override
    public long startTime(final int index) {
        return timers.startTime(index);
    }

    @Override
    public long timeout(final int index) {
        return timers.timeout(index);
    }

    @Override
    public int timerType(final int index) {
        return timers.timerType(index);
    }

    @Override
    public long contextId(final int index) {
        return timers.contextId(index);
    }

    @Override
    public long deadline(final int index) {
        return timers.deadline(index);
    }

    @Override
    public boolean removeById(final long timerId) {
        return timers.removeById(timerId);
    }

    @Override
    public void remove(final int index) {
        timers.remove(index);
    }

    @Override
    public boolean add(final Event event, final Timer timer) {
        return timers.add(event, timer);
    }

    @Override
    public boolean add(final long timerId, final Style style, final int repetition, final long startTime,
                       final long timeout, final int timerType, final long contextId) {
        return timers.add(timerId, style, repetition, startTime, timeout, timerType, contextId);
    }

    @Override
    public void removeAll() {
        timers.removeAll();
    }

    @Override
    public void updateRepetitionById(long timerId, final int repetition) {
        timers.updateRepetitionById(timerId, repetition);
    }

    @Override
    public int indexOfNextDeadline() {
        final int count = count();
        int index = -1;
        long minDeadline = TimeSource.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            final long deadline = deadline(i);
            if (deadline < minDeadline) {
                index = i;
                minDeadline = deadline;
            }
        }
        return index;
    }

    @Override
    public String toString() {
        if (count() == 0) {
            return "SimpleTimerState{}";
        }
        return "SimpleTimerState{next=" + timerId(indexOfNextDeadline()) + ", timers=" + timers + "}";
    }
}
