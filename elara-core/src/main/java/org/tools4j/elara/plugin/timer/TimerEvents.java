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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventRouter;

import static org.tools4j.elara.plugin.timer.TimerEventDescriptor.*;

/**
 * Timer events applying the timer state change through {@link TimerEventApplier}.
 */
public enum TimerEvents {
    ;

    /** Event type for event indicating a timer has expired, usually triggered by a {@link TimerCommands#TRIGGER_TIMER TRIGGER_TIMER} command.*/
    public static final int TIMER_EXPIRED = -10;
    /** Event type for event indicating that a timer has been started.*/
    public static final int TIMER_STARTED = -11;
    /** Event type for event indicating that a timer has been stopped.*/
    public static final int TIMER_STOPPED = -12;

    public static void timerStarted(final MutableDirectBuffer payloadBuffer,
                                    final int offset,
                                    final long timerId,
                                    final int timerType,
                                    final long timeout,
                                    final EventRouter eventRouter) {
        timerEvent(payloadBuffer, offset, TIMER_STARTED, timerId, timerType, timeout, eventRouter);
    }

    public static void timerStopped(final MutableDirectBuffer payloadBuffer,
                                    final int offset,
                                    final long timerId,
                                    final int timerType,
                                    final long timeout,
                                    final EventRouter eventRouter) {
        timerEvent(payloadBuffer, offset, TIMER_STOPPED, timerId, timerType, timeout, eventRouter);
    }

    public static void timerExpired(final Command command, final EventRouter eventRouter) {
        if (command.type() != TimerCommands.TRIGGER_TIMER) {
            throw new IllegalArgumentException("Expected " + TimerCommands.TRIGGER_TIMER + " command but found " + command.type());
        }
        final DirectBuffer payload = command.payload();
        eventRouter.routeEvent(TIMER_EXPIRED, payload, 0, payload.capacity());
    }

    private static void timerEvent(final MutableDirectBuffer payloadBuffer,
                                   final int offset,
                                   final int eventType,
                                   final long timerId,
                                   final int timerType,
                                   final long timeout,
                                   final EventRouter eventRouter) {
        payloadBuffer.putLong(offset + TIMER_ID_OFFSET, timerId);
        payloadBuffer.putInt(offset + TIMER_TYPE_OFFSET, timerType);
        payloadBuffer.putLong(offset + TIMER_TIMEOUT_OFFSET, timeout);
        eventRouter.routeEvent(eventType, payloadBuffer, offset, TIMER_PAYLOAD_SIZE);
    }

    public static long timerId(final Event event) {
        return event.payload().getLong(TIMER_ID_OFFSET);
    }
    public static int timerType(final Event event) {
        return event.payload().getInt(TIMER_TYPE_OFFSET);
    }
    public static long timerTimeout(final Event event) {
        return event.payload().getLong(TIMER_TIMEOUT_OFFSET);
    }
}
