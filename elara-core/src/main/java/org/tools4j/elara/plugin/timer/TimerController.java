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

import org.tools4j.elara.time.TimeSource;

/**
 * Controller API of the {@link TimerPlugin} to start and cancel timers.
 * <p>
 * The controller can be accessed directly through the plugin through one of the {@code TimerPlugin.controller(..)}
 * methods.  Depending on the scope
 */
public interface TimerController extends TimeSource {
    int DEFAULT_TYPE = 0;
    long DEFAULT_CONTEXT_ID = 0;

    /**
     * Returns the current time, save for use also in an event context.  Returns the event time if an event is currently
     * processed or applied, and the actual time from the application {@link TimeSource} if processing a command or when
     * polling an input.
     *
     * @return the event time an event is currently applied or processed, or the actual time otherwise
     */
    @Override
    long currentTime();

    long startAlarm(long time);
    long startAlarm(long time, int type, long contextId);
    long startTimer(long timeout);
    long startTimer(long timeout, int type, long contextId);
    long startPeriodic(long timeout);
    long startPeriodic(long timeout, int type, long contextId);
    boolean cancelTimer(long id);

    interface Default extends TimerController {
        @Override
        default long startAlarm(final long time) {
            return startAlarm(time, DEFAULT_TYPE, DEFAULT_CONTEXT_ID);
        }
        @Override
        default long startTimer(final long timeout) {
            return startTimer(timeout, DEFAULT_TYPE, DEFAULT_CONTEXT_ID);
        }

        @Override
        default long startPeriodic(final long timeout) {
            return startPeriodic(timeout, DEFAULT_TYPE, DEFAULT_CONTEXT_ID);
        }
    }

    interface ControlContext extends Default, AutoCloseable {
        @Override
        void close();
    }
}
