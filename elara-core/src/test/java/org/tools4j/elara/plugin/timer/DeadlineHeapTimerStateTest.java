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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.plugin.timer.TimerState.REPETITION_SINGLE;

/**
 * Unit test for {@link DeadlineHeapTimerState}
 */
class DeadlineHeapTimerStateTest {

    private final long idOffset = 111000000000000L;
    private final int typeOffset = 1000;
    private final long t0 = 999000000000L;
    private final long[] time = {t0 + 1, t0 + 1000, t0 + 2, t0 + 100, t0 + 3};
    private final long[] timeout = {     1,        10,     20,       30,      8};
    private final int[] sorted = {0, 4, 2, 3, 1};

    private TimerState.Mutable timerState;

    @BeforeEach
    public void init() {
        timerState = new DeadlineHeapTimerState();
    }

    @Test
    public void addAndGet() {
        //when
        for (int i = 0; i < time.length; i++) {
            timerState.add(idOffset + i, typeOffset + i, REPETITION_SINGLE, time[i], timeout[i]);
        }

        //then
        assertEquals(time.length, timerState.count(), "count");
        assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
        for (int i = 0; i < time.length; i++) {
            assertTrue(timerState.hasTimer(idOffset + i), "hasTimer(" + (idOffset + i) + ")");
            final int index = timerState.indexById(idOffset + i);
            assertTrue(index >= 0, "indexById(" + (idOffset + i) + ") >= 0");
            assertEquals(idOffset + i, timerState.id(index), "id(" + (idOffset + i) + ")");
            assertEquals(typeOffset + i, timerState.type(index), "type(" + (idOffset + i) + ")");
            assertEquals(REPETITION_SINGLE, timerState.repetition(index), "repetition(" + (idOffset + i) + ")");
            assertEquals(1, timerState.nextRepetition(index), "nextRepetition(" + (idOffset + i) + ")");
            assertEquals(time[i], timerState.time(index), "time(" + (idOffset + i) + ")");
            assertEquals(timeout[i], timerState.timeout(index), "timeout(" + (idOffset + i) + ")");
            assertEquals(time[i] + timeout[i], timerState.deadline(index), "deadline(" + (idOffset + i) + ")");
        }
    }

    @Test
    public void remove() {
        //when
        for (int i = 0; i < time.length; i++) {
            timerState.add(idOffset + i, typeOffset + i, REPETITION_SINGLE, time[i], timeout[i]);
        }

        int removed = 0;
        for (final int i : sorted) {
            //then
            assertEquals(0, timerState.indexById(idOffset + i), "indexById(" + (idOffset + i) + ")");
            assertEquals(idOffset + i, timerState.id(0), "id(0)");
            assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");

            //when
            if (i % 2 == 0) {
                timerState.remove(0);
            } else {
                timerState.removeById(idOffset + i);
            }
            removed++;

            //then
            assertFalse(timerState.hasTimer(idOffset + i), "hasTimer(" + (idOffset + i) + ")");
            assertEquals(-1, timerState.indexById(idOffset + i), "indexById(" + (idOffset + i) + ")");
            assertEquals(time.length - removed, timerState.count(), "count");
        }

        //then
        assertEquals(0, timerState.count(), "count");
        assertEquals(-1, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
    }

    @Test
    public void repeat() {
        //when
        for (int i = 0; i < time.length; i++) {
            timerState.add(idOffset + i, typeOffset + i, 0, time[i], timeout[i]);
        }

        for (final int i : sorted) {
            //then
            assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
            assertEquals(0, timerState.repetition(0), "repetition(" + (idOffset + i) + ")");
            assertEquals(1, timerState.nextRepetition(0), "nextRepetition(" + (idOffset + i) + ")");
        }

        for (int rep = 1; rep <= 9; rep++) {
            //when
            timerState.repetition(0, rep);

            //then
            assertEquals(idOffset + 0, timerState.id(0),  "indexOfNextDeadline()");
            assertEquals(0, timerState.indexOfNextDeadline(), "indexOfNextDeadline()");
            assertEquals(rep, timerState.repetition(0), "repetition(0)");
            assertEquals(rep + 1, timerState.nextRepetition(0), "nextRepetition(0)");
            assertEquals(time[0] + timeout[0] * (rep + 1), timerState.deadline(0), "deadline(0)");
        }

        //when
        timerState.repetition(0, timerState.repetition(0) + 1);

        //then
        assertEquals(idOffset + 4, timerState.id(0), "indexOfNextDeadline()");

        //given
        timerState.repetition(1, timerState.repetition(1) - 2);

        for (int i = 0; i < sorted.length - 1; i++) {
            //when
            timerState.repetition(0, timerState.repetition(0) + 100000);

            //then
            assertEquals(idOffset + sorted[i + 1], timerState.id(0), "indexOfNextDeadline()");
        }

    }

}