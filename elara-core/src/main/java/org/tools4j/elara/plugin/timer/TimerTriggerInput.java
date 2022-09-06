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

import org.tools4j.elara.input.Input;
import org.tools4j.elara.send.CommandSender.SendingContext;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerCommands.TRIGGER_TIMER;
import static org.tools4j.elara.plugin.timer.TimerCommands.triggerTimer;

public final class TimerTriggerInput implements Input {

    private final int source;
    private final TimeSource timeSource;
    private final TimerState timerState;

    private boolean timerTriggerPending;

    public TimerTriggerInput(final int source, final TimeSource timeSource, final TimerState timerState) {
        this.source = source;
        this.timeSource = requireNonNull(timeSource);
        this.timerState = requireNonNull(timerState);
    }

    void timerEventApplied() {
        timerTriggerPending = false;
    }

    @Override
    public int poll(final SenderSupplier senderSupplier) {
        if (timerTriggerPending) {
            return 0;
        }
        final int next = timerState.indexOfNextDeadline();
        if (next >= 0 && timerState.deadline(next) <= timeSource.currentTime()) {
            try (final SendingContext context = senderSupplier.senderFor(source).sendingCommand(TRIGGER_TIMER)) {
                final int length = triggerTimer(context.buffer(), 0, timerState.id(next),
                        timerState.type(next), timerState.repetition(next), timerState.timeout(next));
                context.send(length);
                timerTriggerPending = true;
            }
            return 1;
        }
        //NOTE: - we did not poll anything but we always perform some work by checking the time
        //      - as we are returning zero the idle strategy could kick in and force the duty cycle loop into a pause
        //      - a reasonably configured idle strategy should never cause any serious problems for most timers
        return 0;
    }
}
