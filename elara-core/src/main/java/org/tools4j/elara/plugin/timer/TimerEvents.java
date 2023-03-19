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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.FrameType;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;

import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.PAYLOAD_SIZE;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_ID_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_REPETITION_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TIMEOUT_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TYPE_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerState.REPETITION_SINGLE;

/**
 * Timer events applying the timer state change through {@link TimerEventApplier}.
 */
public enum TimerEvents {
    ;

    /**
     * Event type for event indicating a timer has fired;  the timer has remaining repetitions and will fire again.
     * This event is usually triggered by a {@link TimerCommands#TRIGGER_TIMER TRIGGER_TIMER} command.
     */
    public static final int TIMER_FIRED = -10;
    /**
     * Event type for event indicating a timer has fired and expired;  it is single fire event of a one-off timer or the
     * last fire event of a periodic timer.
     * This event is usually triggered by a {@link TimerCommands#TRIGGER_TIMER TRIGGER_TIMER} command.
     */
    public static final int TIMER_EXPIRED = -11;
    /** Event type for event indicating that a timer has been started.*/
    public static final int TIMER_STARTED = -12;
    /** Event type for event indicating that a timer has been stopped.*/
    public static final int TIMER_STOPPED = -13;

    public static void routeTimerStarted(final long timerId,
                                         final int timerType,
                                         final long timeout,
                                         final EventRouter eventRouter) {
        try (final RoutingContext context = eventRouter.routingEvent(TIMER_STARTED)) {
            context.route(timerStarted(context.buffer(), 0, timerId, timerType, timeout));
        }
    }

    public static void routePeriodicStarted(final long timerId,
                                            final int timerType,
                                            final long timeout,
                                            final EventRouter eventRouter) {
        try (final RoutingContext context = eventRouter.routingEvent(TIMER_STARTED)) {
            context.route(periodicStarted(context.buffer(), 0, timerId, timerType, timeout));
        }
    }

    public static void routeTimerStopped(final long timerId,
                                         final int timerType,
                                         final int repetition,
                                         final long timeout,
                                         final EventRouter eventRouter) {
        try (final RoutingContext context = eventRouter.routingEvent(TIMER_STOPPED)) {
            context.route(timerStopped(context.buffer(), 0, timerId, timerType, repetition, timeout));
        }
    }

    public static int timerStarted(final MutableDirectBuffer buffer,
                                   final int offset,
                                   final long timerId,
                                   final int timerType,
                                   final long timeout) {
        return timerEvent(buffer, offset, timerId, timerType, REPETITION_SINGLE, timeout);
    }

    public static int periodicStarted(final MutableDirectBuffer buffer,
                                      final int offset,
                                      final long timerId,
                                      final int timerType,
                                      final long timeout) {
        return timerEvent(buffer, offset, timerId, timerType, 0, timeout);
    }

    public static int timerStopped(final MutableDirectBuffer buffer,
                                   final int offset,
                                   final long timerId,
                                   final int timerType,
                                   final int repetition,
                                   final long timeout) {
        return timerEvent(buffer, offset, timerId, timerType, repetition, timeout);
    }

    public static int timerFired(final MutableDirectBuffer buffer, final int offset, final Command command) {
        return timerTriggered(buffer, offset, command);
    }

    public static int timerExpired(final MutableDirectBuffer buffer, final int offset, final Command command) {
        return timerTriggered(buffer, offset, command);
    }

    private static int timerTriggered(final MutableDirectBuffer buffer,
                                      final int offset,
                                      final Command command) {
        if (command.payloadType() != TimerCommands.TRIGGER_TIMER) {
            throw new IllegalArgumentException("Expected " + TimerCommands.TRIGGER_TIMER + " command but found " + command.payloadType());
        }
        final DirectBuffer payload = command.payload();
        buffer.putBytes(offset, payload, 0, payload.capacity());
        return payload.capacity();
    }

    private static int timerEvent(final MutableDirectBuffer buffer,
                                  final int offset,
                                  final long timerId,
                                  final int timerType,
                                  final int repetition,
                                  final long timeout) {
        buffer.putLong(offset + TIMER_ID_OFFSET, timerId);
        buffer.putInt(offset + TIMER_TYPE_OFFSET, timerType);
        buffer.putInt(offset + TIMER_REPETITION_OFFSET, repetition);
        buffer.putLong(offset + TIMER_TIMEOUT_OFFSET, timeout);
        return PAYLOAD_SIZE;
    }

    public static long timerId(final Event event) {
        return TimerPayloadDescriptor.timerId(event.payload());
    }

    public static int timerType(final Event event) {
        return TimerPayloadDescriptor.timerType(event.payload());
    }

    public static int timerRepetition(final Event event) {
        return TimerPayloadDescriptor.timerRepetition(event.payload());
    }

    public static long timerTimeout(final Event event) {
        return TimerPayloadDescriptor.timerTimeout(event.payload());
    }

    public static boolean isTimerEvent(final Event event) {
        return isTimerEvent(event.payloadType());
    }

    public static boolean isTimerEvent(final DataFrame frame) {
        return FrameType.isAppRoutedEventType(frame.type()) && isTimerEvent(frame.payloadType());
    }

    public static boolean isTimerEvent(final int payloadType) {
        switch (payloadType) {
            case TIMER_EXPIRED://fallthrough
            case TIMER_FIRED://fallthrough
            case TIMER_STARTED://fallthrough
            case TIMER_STOPPED://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String timerEventName(final Event event) {
        return timerEventName(event.payloadType());
    }

    public static String timerEventName(final DataFrame frame) {
        return timerEventName(frame.payloadType());
    }

    public static String timerEventName(final int payloadType) {
        switch (payloadType) {
            case TIMER_EXPIRED:
                return "TIMER_EXPIRED";
            case TIMER_FIRED:
                return "TIMER_FIRED";
            case TIMER_STARTED:
                return "TIMER_STARTED";
            case TIMER_STOPPED:
                return "TIMER_STOPPED";
            default:
                throw new IllegalArgumentException("Not a timer event type: " + payloadType);
        }
    }
}
