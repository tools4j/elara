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

import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver.ReceivingContext;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerCommands.TRIGGER_TIMER;
import static org.tools4j.elara.plugin.timer.TimerCommands.triggerTimer;

public final class TimerTrigger {

    private final TimerState timerState;

    private long nextTriggerTime = TimeSource.BIG_BANG;

    public TimerTrigger(final TimerState timerState) {
        this.timerState = requireNonNull(timerState);
    }

    public Output asOutput() {
        return (event, replay, loopback) -> {
            if (event.time() >= nextTriggerTime) {
                final int next = timerState.indexOfNextDeadline();
                if (next >= 0 && timerState.deadline(next) <= event.time()) {
                    try (final CommandLoopback.EnqueuingContext context = loopback.enqueuingCommand(TRIGGER_TIMER)) {
                        final int length = triggerTimer(context.buffer(), 0, timerState.id(next),
                                timerState.type(next), timerState.repetition(next), timerState.timeout(next));
                        context.enqueue(length);
                    }
                }
            }
            nextTriggerTime = TimerEvents.isTimerEvent(event) ? event.time() : event.time() + 1;
        };
    }

    public Input asInput(final int commandSource, final TimeSource timeSource) {
        requireNonNull(timeSource);
        final SequenceGenerator sequenceGenerator = new SimpleSequenceGenerator();
        return receiver -> {
            final long time = timeSource.currentTime();
            if (time >= nextTriggerTime) {
                final int next = timerState.indexOfNextDeadline();
                if (next >= 0 && timerState.deadline(next) <= time) {
                    final long seq = sequenceGenerator.nextSequence();
                    try (final ReceivingContext context = receiver.receivingMessage(commandSource, seq, TRIGGER_TIMER)) {
                        final int length = triggerTimer(context.buffer(), 0, timerState.id(next),
                                timerState.type(next), timerState.repetition(next), timerState.timeout(next));
                        context.receive(length);
                    }
                }
                nextTriggerTime = time + 1;
                return 1;
            }
            return 0;
        };
    }
}
