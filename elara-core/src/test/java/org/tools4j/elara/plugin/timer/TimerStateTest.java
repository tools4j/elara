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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tools4j.elara.plugin.timer.Timer.Style;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link TimerState} implementations.
 */
class TimerStateTest {

    private final int repetition = 0;

    private final long idOffset = 111000000000000L;
    private final int typeOffset = 1000;
    private final long contextIdOffset = 6660000000L;
    private final long t0 = 999000000000L;
    private final long[] time = {t0 + 1, t0 + 1000, t0 + 2, t0 + 100, t0 + 3, t0 + 10000, t0 + 4};
    private final long[] timeout = {1, 10, 20, 30, 8, 100, 40};
    private final int[] sorted = {0, 4, 2, 6, 3, 1, 5};

    @ParameterizedTest
    @ValueSource(classes = {SimpleTimerState.class, DeadlineHeapTimerState.class})
    public void addAndGet(final Class<? extends MutableTimerState> timerStateClass) throws Exception {
        //given
        final MutableTimerState timerState = timerStateClass.newInstance();

        //when
        for (int i = 0; i < time.length; i++) {
            timerState.add(idOffset + i, Style.TIMER, 0, time[i], timeout[i], typeOffset + i, contextIdOffset + i);
        }

        //then
        assertEquals(time.length, timerState.count(), "count");
        assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
        for (int i = 0; i < time.length; i++) {
            assertTrue(timerState.hasTimer(idOffset + i), "hasTimer(" + (idOffset + i) + ")");
            final int index = timerState.index(idOffset + i);
            assertTrue(index >= 0, "index(" + (idOffset + i) + ") >= 0");
            assertEquals(idOffset + i, timerState.timerId(index), "timerId(" + (idOffset + i) + ")");
            assertEquals(Style.TIMER, timerState.style(index), "style(" + (idOffset + i) + ")");
            assertEquals(repetition, timerState.repetition(index), "repetition(" + (idOffset + i) + ")");
            assertEquals(time[i], timerState.startTime(index), "startTime(" + (idOffset + i) + ")");
            assertEquals(timeout[i], timerState.timeout(index), "timeout(" + (idOffset + i) + ")");
            assertEquals(time[i] + timeout[i], timerState.deadline(index), "deadline(" + (idOffset + i) + ")");
            assertEquals(typeOffset + i, timerState.timerType(index), "timerType(" + (idOffset + i) + ")");
            assertEquals(contextIdOffset + i, timerState.contextId(index), "contextId(" + (idOffset + i) + ")");
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {SimpleTimerState.class, DeadlineHeapTimerState.class})
    public void remove(final Class<? extends MutableTimerState> timerStateClass) throws Exception {
        //given
        final MutableTimerState timerState = timerStateClass.newInstance();

        //when
        for (int i = 0; i < time.length; i++) {
            timerState.add(idOffset + i, Style.TIMER, repetition, time[i], timeout[i], typeOffset + i, contextIdOffset + i);
        }

        int removed = 0;
        for (final int i : sorted) {
            //then
            final int indexOfNextDeadline = timerState.indexOfNextDeadline();
            assertEquals(indexOfNextDeadline, timerState.index(idOffset + i), "index(" + (idOffset + i) + ")");
            assertEquals(idOffset + i, timerState.timerId(indexOfNextDeadline), "timerId(indexOfNextDeadline())");

            //when
            if (i % 2 == 0) {
                timerState.remove(indexOfNextDeadline);
            } else {
                timerState.removeById(idOffset + i);
            }
            removed++;

            //then
            assertFalse(timerState.hasTimer(idOffset + i), "hasTimer(" + (idOffset + i) + ")");
            assertEquals(-1, timerState.index(idOffset + i), "index(" + (idOffset + i) + ")");
            assertEquals(time.length - removed, timerState.count(), "count");
        }

        //then
        assertEquals(0, timerState.count(), "count");
        assertEquals(-1, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
    }

    @ParameterizedTest
    @ValueSource(classes = {SimpleTimerState.class, DeadlineHeapTimerState.class})
    public void repeat(final Class<? extends MutableTimerState> timerStateClass) throws Exception {
        //given
        final MutableTimerState timerState = timerStateClass.newInstance();

        //when
        for (int i = 0; i < time.length; i++) {
            timerState.add(idOffset + i, Style.TIMER, 0, time[i], timeout[i], typeOffset + i, contextIdOffset + i);
        }

        //then
        assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
        for (int i = 0; i < time.length; i++) {
            assertEquals(0, timerState.repetition(i), "repetition(" + i + ")");
        }

        //when: update first 9x does not change the heap
        for (int rep = 1; rep <= 9; rep++) {
            //when
            timerState.updateRepetitionById(idOffset + sorted[0], rep);

            //then
            assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
            assertEquals(idOffset + sorted[0], timerState.timerId(0),  "timerId(0)");
            assertEquals(rep, timerState.repetition(0), "repetition(0)");
            assertEquals(time[0] + timeout[0] * (rep + 1), timerState.deadline(0), "deadline(0)");
        }

        //when: update 10th time moves it one down
        timerState.updateRepetitionById(idOffset + sorted[0], timerState.repetition(0) + 1);

        //then
        final int indexOfNextDeadline = timerState.indexOfNextDeadline();
        assertEquals(idOffset + sorted[1], timerState.timerId(indexOfNextDeadline), "timerId(indexOfNextDeadline())");

        //when: reduce repetition by 2 brings it back to top
        final int repetition = timerState.repetition(timerState.index(idOffset + sorted[0]));
        timerState.updateRepetitionById(idOffset + sorted[0], repetition - 2);

        //then
        assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
        assertEquals(idOffset + sorted[0], timerState.timerId(0), "timerId(0)");

        //when: update in sorted order to move it past all others (but the already updated ones)
        for (int i = 0; i < sorted.length - 1; i++) {
            final long timerId = idOffset + sorted[i];
            final int curIndex = timerState.indexOfNextDeadline();
            assertEquals(timerId, timerState.timerId(curIndex), "timerId(indexOfNextDeadline())");

            //when
            timerState.updateRepetitionById(timerId, timerState.repetition(0) + 100000);

            //then
            final long nextTimerId = idOffset + sorted[i + 1];
            final int nextIndex = timerState.indexOfNextDeadline();
            assertEquals(nextTimerId, timerState.timerId(nextIndex), "timerId(indexOfNextDeadline())");
        }
    }

    @ParameterizedTest
    @ValueSource(classes = {SimpleTimerState.class, DeadlineHeapTimerState.class})
    public void removeAndSeek(final Class<? extends MutableTimerState> timerStateClass) throws Exception {
        //given
        //            1
        //        10     2
        //      11  12 3   4
        final long time = 123400000000L;
        final MutableTimerState timerState = timerStateClass.newInstance();
        final Consumer<MutableTimerState> initializer = state -> {
            state.removeAll();
            for (final long timeout : new long[]{1, 10, 2, 11, 12, 3, 4}) {
                final long id = timeout;
                timerState.add(id, Style.TIMER, 0, time, timeout, 0, 0);
            }
        };
        initializer.accept(timerState);

        //when: remove 2
        timerState.removeById(2);

        //then
        //            1                              1
        //        10     4    --sweep down-->    10     3
        //      11  12 3                       11  12 4
        for (final long timeout : new long[]{1, 3, 4, 10, 11, 12}) {
            final long id = timeout;
            final int index = timerState.indexOfNextDeadline();
            assertEquals(id, timerState.timerId(index));
            assertEquals(timeout, timerState.timeout(index));
            assertTrue(timerState.removeById(id));
        }
        assertEquals(0, timerState.count());

        //when: remove 2, then 11, then add 100
        initializer.accept(timerState);
        timerState.removeById(2);
        timerState.removeById(11);
        timerState.add(100, Style.TIMER, 0, time, 100, 0, 0);

        //then
        //            1                           1                            1
        //        10     3   --sweep up-->     4     3   --add: 100-->     4       3
        //      4   12                      10  12                      10  12  100
        for (final long timeout : new long[]{1, 3, 4, 10, 12, 100}) {
            final long id = timeout;
            final int index = timerState.indexOfNextDeadline();
            assertEquals(id, timerState.timerId(index));
            assertEquals(timeout, timerState.timeout(index));
            assertTrue(timerState.removeById(id));
        }
        assertEquals(0, timerState.count());
    }
}