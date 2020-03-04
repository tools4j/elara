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
import org.tools4j.elara.init.Launcher;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class TimerApplicationTest {

    @Test
    public void singleTimers() {
        final TimerApplication app = new TimerApplication();
        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
        commands.add(TimerApplication.startTimer(500));
        commands.add(TimerApplication.startTimer(1000));
        commands.add(TimerApplication.startTimer(2000));
        commands.add(TimerApplication.startTimer(5000));
        try (final Launcher launcher = app.launch(commands)) {
            while (!commands.isEmpty()) {
                launcher.join(20);
            }
            launcher.join(6000);
        }
    }

    @Test
    public void periodicTimer() {
        final TimerApplication app = new TimerApplication();
        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
        commands.add(TimerApplication.startPeriodic(2000));
        try (final Launcher launcher = app.launch(commands)) {
            launcher.join(22000);
        }
    }

}