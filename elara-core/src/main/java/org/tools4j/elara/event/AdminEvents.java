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
package org.tools4j.elara.event;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.CommandType;

public enum AdminEvents {
    ;

    private static final DirectBuffer EMPTY_BUFFER = new UnsafeBuffer(0, 0);

    public static final int TIMER_TYPE_OFFSET = 0;
    public static final int TIMER_TYPE_LENGTH = Integer.BYTES;
    public static final int TIMER_ID_OFFSET = TIMER_TYPE_OFFSET + TIMER_TYPE_LENGTH;
    public static final int TIMER_ID_LENGTH = Long.BYTES;
    public static final int TIMER_TIMEOUT_OFFSET = TIMER_ID_OFFSET + TIMER_ID_LENGTH;
    public static final int TIMER_TIMEOUT_LENGTH = Long.BYTES;
    public static final int TIMER_PAYLOAD_SIZE = TIMER_TYPE_LENGTH + TIMER_ID_LENGTH +
            TIMER_TIMEOUT_LENGTH;

    public static FlyweightEvent noop(final FlyweightEvent flyweightEvent,
                                      final MutableDirectBuffer headerBuffer,
                                      final int offset,
                                      final Command command,
                                      final int index) {
        return flyweightEvent.init(headerBuffer, offset, command.id().input(), command.id().sequence(), index,
                EventType.NOOP.value(), command.time(), EMPTY_BUFFER, 0, 0);
    }

    public static void timerStarted(final MutableDirectBuffer payloadBuffer,
                                    final int offset,
                                    final int timerType,
                                    final long timerId,
                                    final long timeout,
                                    final EventRouter eventRouter) {
        timerEvent(payloadBuffer, offset, EventType.TIMER_STARTED, timerType, timerId, timeout, eventRouter);
    }

    public static void timerStopped(final MutableDirectBuffer payloadBuffer,
                                    final int offset,
                                    final int timerType,
                                    final long timerId,
                                    final long timeout,
                                    final EventRouter eventRouter) {
        timerEvent(payloadBuffer, offset, EventType.TIMER_STOPPED, timerType, timerId, timeout, eventRouter);
    }

    public static void timerExpired(final Command command, final EventRouter eventRouter) {
        if (command.type() != CommandType.TRIGGER_TIMER.value()) {
            throw new IllegalArgumentException("Expected " + CommandType.TRIGGER_TIMER + " command but found " + command.type());
        }
        final DirectBuffer payload = command.payload();
        eventRouter.routeEvent(EventType.TIMER_EXPIRED.value(), payload, 0, payload.capacity());
    }

    private static void timerEvent(final MutableDirectBuffer payloadBuffer,
                                   final int offset,
                                   final EventType eventType,
                                   final int timerType,
                                   final long timerId,
                                   final long timeout,
                                   final EventRouter eventRouter) {
        payloadBuffer.putInt(offset + TIMER_TYPE_OFFSET, timerType);
        payloadBuffer.putLong(offset + TIMER_ID_OFFSET, timerId);
        payloadBuffer.putLong(offset + TIMER_TIMEOUT_OFFSET, timeout);
        eventRouter.routeEvent(eventType.value(), payloadBuffer, offset, TIMER_PAYLOAD_SIZE);
    }

    public static int timerType(final Event event) {
        return event.payload().getInt(TIMER_TYPE_OFFSET);
    }
    public static long timerId(final Event event) {
        return event.payload().getLong(TIMER_ID_OFFSET);
    }
    public static long timerTimeout(final Event event) {
        return event.payload().getLong(TIMER_TIMEOUT_OFFSET);
    }
}
