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
package org.tools4j.elara.samples.timer;

import org.agrona.DirectBuffer;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.run.ElaraRunner;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.tools4j.elara.samples.timer.TimerApplication.MAX_PERIODIC_REPETITIONS;

class TimerApplicationTest {

    @Test
    public void singleTimers() {
        singleTimers(false);
    }

    @Test
    public void singleTimersPersisted() {
        singleTimers(true);
    }

    private void singleTimers(final boolean persisted) {
        final TimerApplication app = new TimerApplication();
        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
        commands.add(TimerApplication.startTimer(1001, 200));
        commands.add(TimerApplication.startTimer(1002, 500));
        commands.add(TimerApplication.startTimer(1003, 1000));
        commands.add(TimerApplication.startTimer(1004, 1000));
        try (final ElaraRunner runner = persisted ? app.chronicleQueue(commands, "single") : app.inMemory(commands)) {
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(3000);
        }
    }

    @Test
    public void periodicTimer() {
        periodicTimer(false);
    }

    @Test
    public void periodicTimerPersisted() {
        periodicTimer(true);
    }

    private void periodicTimer(final boolean persisted) {
        //given
        final long periodMillis = 500;
        final TimerApplication app = new TimerApplication();
        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();

        //when
        commands.add(TimerApplication.startPeriodic(666666666, periodMillis));
        try (final ElaraRunner runner = persisted ? app.chronicleQueue(commands, "periodic") : app.inMemory(commands)) {

            //then
            runner.join(100 + periodMillis * (MAX_PERIODIC_REPETITIONS + 1));
        }
    }



}