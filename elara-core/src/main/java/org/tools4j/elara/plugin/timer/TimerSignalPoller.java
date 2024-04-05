/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.BitUtil;
import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.plugin.timer.Timer.Style;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.CommandSender.SendingContext;
import org.tools4j.elara.source.SourceContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerCommands.SIGNAL_TIMER;

/**
 * Input poller to send signal commands when a timer fires or expires.
 */
public final class TimerSignalPoller implements InputPoller {
    private final TimeSource timeSource;
    private final TimerState timerState;
    private final int signalInputSkipMask;
    private int counter;

    public TimerSignalPoller(final TimeSource timeSource, final TimerState timerState, final int signalInputSkip) {
        this.timeSource = requireNonNull(timeSource);
        this.timerState = requireNonNull(timerState);
        this.signalInputSkipMask = signalInputSkip - 1;
        if (!BitUtil.isPowerOfTwo(signalInputSkip)) {
            throw new IllegalArgumentException("Invalid signalInputSkip value, must be a power of two: " +
                    signalInputSkip);
        }
    }

    @Override
    public int poll(final SourceContext sourceContext) {
        if ((counter & signalInputSkipMask) != 0) {
            counter++;
            return 0;
        }
        if (sourceContext.commandTracker().hasInFlightCommand()) {
            return 0;
        }
        counter++;

        final int index = timerState.indexOfNextDeadline();
        if (index >= 0 && timerState.deadline(index) <= timeSource.currentTime()) {
            final CommandSender sender = sourceContext.commandSender();
            try (final SendingContext context = sender.sendingCommand(SIGNAL_TIMER)) {
                final Style style = timerState.style(index);
                final int length;
                switch (style) {
                    case ALARM:
                        length = FlyweightTimerPayload.writeAlarm(
                                timerState.timerId(index),
                                timerState.timeout(index),
                                timerState.timerType(index),
                                timerState.contextId(index),
                                context.buffer(), 0
                        );
                        break;
                    case TIMER:
                        length = FlyweightTimerPayload.writeTimer(
                                timerState.timerId(index),
                                timerState.timeout(index),
                                timerState.timerType(index),
                                timerState.contextId(index),
                                context.buffer(), 0
                        );
                        break;
                    case PERIODIC:
                        length = FlyweightTimerPayload.writePeriodic(
                                timerState.timerId(index),
                                timerState.timeout(index),
                                timerState.repetition(index) + 1,
                                timerState.timerType(index),
                                timerState.contextId(index),
                                context.buffer(), 0
                        );
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid style: " + style);
                }
                context.send(length);
            }
            return 1;
        }
        //NOTE: - we did not poll anything, but we always perform some work by checking the time
        //      - as we are returning zero the idle strategy could kick in and force the duty cycle loop into a pause
        //      - a reasonably configured idle strategy should never cause any serious problems for most timers
        return 0;
    }
}
