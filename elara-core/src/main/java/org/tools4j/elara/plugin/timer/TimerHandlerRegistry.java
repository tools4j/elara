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

/**
 * Registry to add and remove instances of {@link TimerEventHandler} and the specialized {@link TimerSignalHandler}.
 * <p>
 * The registry can be accessed through the {@link TimerPlugin} via its {@link TimerPlugin#registry() registry()}
 * method.
 */
public interface TimerHandlerRegistry {
    void addTimerEventHandlerFor(long timerId, TimerEventHandler handler);
    void addTimerEventHandler(TimerEventHandler handler);
    void addTimerEventHandler(int timerType, TimerEventHandler handler);
    void addTimerEventHandler(int timerType, long contextId, TimerEventHandler handler);
    void addTimerEventHandler(long contextId, TimerEventHandler handler);
    void addTimerSignalHandler(TimerSignalHandler handler);
    void addTimerSignalHandler(int timerType, TimerSignalHandler handler);
    void addTimerSignalHandler(int timerType, long contextId, TimerSignalHandler handler);
    void addTimerSignalHandler(long contextId, TimerSignalHandler handler);
    boolean removeTimerEventHandler(TimerEventHandler handler);
    boolean removeTimerEventHandlerFor(long timerId, TimerEventHandler handler);
    boolean removeTimerEventHandler(int timerType, TimerEventHandler handler);
    boolean removeTimerEventHandler(int timerType, long contextId, TimerEventHandler handler);
    boolean removeTimerEventHandler(long contextId, TimerEventHandler handler);
    boolean removeTimerEventHandlers(long contextId);

    /**
     * Removes all timer event handlers.
     *
     * @param dispose if true internal maps for context timers are disposed, otherwise they will be reused
     */
    void clear(boolean dispose);
}
