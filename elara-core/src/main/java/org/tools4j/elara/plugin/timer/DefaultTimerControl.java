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

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.EventRouter;
import org.tools4j.elara.plugin.Plugin;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Controller to simplify routing of timer start and stop events.  Requires application access to timer state which can
 * be acquired by registering the plugin via {@link org.tools4j.elara.init.Context#plugin(Plugin, Function)} and
 * providing the timer state from the application state.
 */
public class DefaultTimerControl implements TimerControl {

    private final MutableDirectBuffer buffer;

    public DefaultTimerControl() {
        this(new ExpandableDirectByteBuffer(TimerEventDescriptor.TIMER_PAYLOAD_SIZE));
    }

    public DefaultTimerControl(final MutableDirectBuffer buffer) {
        this.buffer = requireNonNull(buffer);
    }

    @Override
    public long startTimer(final int type, final long timeout, final TimerState timerState, final EventRouter eventRouter) {
        final long id = nextTimerId(timerState, eventRouter);
        TimerEvents.timerStarted(buffer, 0, type, id, timeout, eventRouter);
        return id;
    }

    @Override
    public boolean stopTimer(final long id, final TimerState timerState, final EventRouter eventRouter) {
        final int count = timerState.count();
        for (int i = 0; i < count; i++) {
            if (id == timerState.id(i)) {
                TimerEvents.timerStopped(buffer, 0, timerState.type(i), id, timerState.timeout(i), eventRouter);
                return true;
            }
        }
        return false;
    }

    public static long nextTimerId(final TimerState timerState, final EventRouter eventRouter) {
        final long maxId = maxTimerId(timerState);
        return Math.max(0, maxId) + 1 + eventRouter.nextEventIndex();
    }

    private static long maxTimerId(final TimerState timerState) {
        long maxId = Long.MIN_VALUE;
        final int count = timerState.count();
        for (int i = 0; i < count; i++) {
            maxId = Long.max(maxId, timerState.id(i));
        }
        return maxId;
    }
}
