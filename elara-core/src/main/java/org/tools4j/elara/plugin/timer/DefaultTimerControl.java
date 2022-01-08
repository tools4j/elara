/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.route.EventRouter;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * Controller to simplify routing of timer start and stop events.  Requires application access to timer state which is
 * available through {@link Configuration#configure()} for instance by registering the timer plugin as follows:
 * <pre>
 *     DefaultTimerControl timerControl = new DefaultTimerControl();
 *     Configuration.configure().plugin(Plugins.timerPlugin(), timerControl);
 * </pre>
 *
 * Other initialisation alternatives are
 * <pre>
 *     DefaultTimerControl timerControl = new DefaultTimerControl(new DeadlineHeapTimerState());
 *     Configuration.configure().plugin(Plugins.timerPlugin(), timerControl.timerState());
 * </pre>
 * or also
 * <pre>
 *     DefaultTimerControl timerControl = new DefaultTimerControl();
 *     Configuration.configure().plugin(Plugins.timerPlugin(), new DeadlineHeapTimerState(), timerControl);
 * </pre>
 *
 * @see Context#plugin(Plugin, Consumer) 
 * @see Context#plugin(Plugin, Supplier)
 * @see Context#plugin(Plugin, Supplier, Consumer)
 */
public class DefaultTimerControl implements TimerControl, Consumer<TimerState> {

    private TimerState timerState;

    public DefaultTimerControl() {
        super();
    }

    public DefaultTimerControl(final Context context) {
        context.plugin(Plugins.timerPlugin(), this);
    }

    public DefaultTimerControl(final TimerState timerState) {
        this.timerState = requireNonNull(timerState);
    }

    @Override
    public void accept(final TimerState timerState) {
        this.timerState = requireNonNull(timerState);
    }

    public TimerState timerState() {
        if (timerState == null) {
            throw new IllegalStateException("timer state is not initialised");
        }
        return timerState;
    }

    @Override
    public boolean startTimer(final long id, final int type, final long timeout, final EventRouter eventRouter) {
        if (timerState().hasTimer(id)) {
            return false;
        }
        TimerEvents.routeTimerStarted(id, type, timeout, eventRouter);
        return true;
    }

    @Override
    public boolean startPeriodic(final long id, final int type, final long timeout, final EventRouter eventRouter) {
        if (timerState().hasTimer(id)) {
            return false;
        }
        TimerEvents.routePeriodicStarted(id, type, timeout, eventRouter);
        return true;
    }

    @Override
    public boolean stopTimer(final long id, final EventRouter eventRouter) {
        final TimerState timerState = timerState();
        final int index = timerState.indexById(id);
        if (index < 0) {
            return false;
        }
        TimerEvents.routeTimerStopped(id, timerState.type(index), timerState.repetition(index),
                timerState.timeout(index), eventRouter);
        return true;
    }

}
