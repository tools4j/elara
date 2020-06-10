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

import org.tools4j.elara.init.Context;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerEvents.periodicStarted;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerStarted;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerStopped;

/**
 * Controller to simplify routing of timer start and stop events.  Requires application access to timer state which is
 * available through {@link Context#plugins()}.
 */
public class DefaultTimerControl implements TimerControl {

    private TimerState timerState;

    public DefaultTimerControl(final Context context) {
        context.plugin(Plugins.timerPlugin(), this::initTimerState);
    }

    private void initTimerState(final TimerState timerState) {
        this.timerState = requireNonNull(timerState);
    }

    public TimerState timerState() {
        if (timerState == null) {
            throw new IllegalStateException("timer state is not initialised");
        }
        return timerState;
    }

    @Override
    public long startTimer(final int type, final long timeout, final EventRouter eventRouter) {
        final long id = nextTimerId(eventRouter);
        try (final RoutingContext context = eventRouter.routingEvent(TimerEvents.TIMER_STARTED)) {
            final int length = timerStarted( context.buffer(), 0, id, type, timeout);
            context.route(length);
        }
        return id;
    }

    @Override
    public long startPeriodic(final int type, final long timeout, final EventRouter eventRouter) {
        final long id = nextTimerId(eventRouter);
        try (final RoutingContext context = eventRouter.routingEvent(TimerEvents.TIMER_STARTED)) {
            final int length = periodicStarted( context.buffer(), 0, id, type, timeout);
            context.route(length);
        }
        return id;
    }

    @Override
    public boolean stopTimer(final long id, final EventRouter eventRouter) {
        final TimerState timerState = timerState();
        final int index = timerState.indexById(id);
        if (index >= 0) {
            try (final RoutingContext context = eventRouter.routingEvent(TimerEvents.TIMER_STOPPED)) {
                final int length = timerStopped( context.buffer(), 0, id, timerState.type(index),
                        timerState.repetition(index), timerState.timeout(index));
                context.route(length);
            }
            return true;
        }
        return false;
    }

    @Override
    public long nextTimerId(final EventRouter eventRouter) {
        final long maxId = maxTimerId(timerState());
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
