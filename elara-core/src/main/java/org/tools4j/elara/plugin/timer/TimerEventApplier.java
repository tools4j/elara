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

import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.timer.Timer.Style;

import static java.util.Objects.requireNonNull;

public class TimerEventApplier implements EventApplier {

    private final TimerPlugin timerPlugin;
    private final MutableTimerState timerState;
    private final TimerIdGenerator timerIdGenerator;
    private final FlyweightTimerPayload timer = new FlyweightTimerPayload();

    public TimerEventApplier(final TimerPlugin timerPlugin,
                             final MutableTimerState timerState,
                             final TimerIdGenerator timerIdGenerator) {
        this.timerPlugin = requireNonNull(timerPlugin);
        this.timerState = requireNonNull(timerState);
        this.timerIdGenerator = requireNonNull(timerIdGenerator);
    }

    @Override
    public void onEvent(final Event event) {
        switch (event.payloadType()) {
            case TimerEvents.TIMER_STARTED:
                timer.wrap(event.payload(), 0);
                timerState.add(event, timer.wrap(event.payload(), 0));
                updateTimerIdSequence(timer.timerId());
                notifyTimerEvent(event);
                break;
            case TimerEvents.TIMER_CANCELLED:
                timer.wrap(event.payload(), 0);
                timerState.removeById(timer.timerId());
                notifyTimerEvent(event);
                break;
            case TimerEvents.TIMER_SIGNALLED: {
                timer.wrap(event.payload(), 0);
                if (timer.style() == Style.PERIODIC) {
                    timerState.updateRepetitionById(timer.timerId(), timer.repetition());
                } else {
                    timerState.removeById(timer.timerId());
                }
                notifyTimerEvent(event);
                break;
            }
        }
    }

    private void updateTimerIdSequence(final long timerId) {
        timerIdGenerator.next(timerId + 1);
    }

    private void notifyTimerEvent(final Event event) {
        try {
            timerPlugin.onTimerEvent(event, timer);
        } finally {
            timer.reset();
        }
    }
}
