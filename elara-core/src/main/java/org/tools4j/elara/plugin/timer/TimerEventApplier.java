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

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerId;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerRepetition;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerTimeout;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerType;

public class TimerEventApplier implements EventApplier {

    private final TimerState.Mutable timerState;
    private final TimerTriggerInput timerTriggerInput;

    public TimerEventApplier(final TimerState.Mutable timerState, final TimerTriggerInput timerTriggerInput) {
        this.timerState = requireNonNull(timerState);
        this.timerTriggerInput = requireNonNull(timerTriggerInput);
    }

    @Override
    public void onEvent(final Event event) {
        switch (event.type()) {
            case TimerEvents.TIMER_STARTED:
                if (timerState.add(
                        timerId(event), timerType(event), timerRepetition(event), event.time(), timerTimeout(event)
                )) {
                    timerTriggerInput.timerEventApplied();
                }
                break;
            case TimerEvents.TIMER_FIRED: {
                final int index = timerState.indexById(timerId(event));
                timerState.repetition(index, timerState.repetition(index) + 1);
                timerTriggerInput.timerEventApplied();
                break;
            }
            case TimerEvents.TIMER_EXPIRED://fall through
            case TimerEvents.TIMER_STOPPED:
                if (timerState.removeById(timerId(event))) {
                    timerTriggerInput.timerEventApplied();
                }
                break;
        }
    }
}
