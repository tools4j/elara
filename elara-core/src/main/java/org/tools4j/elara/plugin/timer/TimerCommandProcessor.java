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

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerExpired;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerFired;
import static org.tools4j.elara.plugin.timer.TimerState.REPETITION_SINGLE;

public class TimerCommandProcessor implements CommandProcessor {

    private final TimerState timerState;

    public TimerCommandProcessor(final TimerState timerState) {
        this.timerState = requireNonNull(timerState);
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        if (command.type() == TimerCommands.TRIGGER_TIMER) {
            final long timerId = TimerCommands.timerId(command);
            if (timerState.hasTimer(timerId)) {
                final int repetition = TimerCommands.timerRepetition(command);
                if (repetition == REPETITION_SINGLE) {
                    try (final RoutingContext context = router.routingEvent(TimerEvents.TIMER_EXPIRED)) {
                        final int length = timerExpired(context.buffer(), 0, command);
                        context.route(length);
                    }
                } else {
                    try (final RoutingContext context = router.routingEvent(TimerEvents.TIMER_FIRED)) {
                        final int length = timerFired(context.buffer(), 0, command);
                        context.route(length);
                    }
                }
            }
        }
    }
}
