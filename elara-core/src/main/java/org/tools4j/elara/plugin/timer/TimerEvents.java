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

import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.PAYLOAD_SIZE;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_ID_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_REPETITION_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TIMEOUT_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TYPE_OFFSET;

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

    public static int timerStarted(final MutableDirectBuffer buffer,
                                   final int offset,
                                   final long timerId,
                                   final int timerType,
                                   final int repetition,
                                   final long timeout) {
        return timerEvent(buffer, offset, timerId, timerType, repetition, timeout);
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
        if (command.type() != TimerCommands.TRIGGER_TIMER) {
            throw new IllegalArgumentException("Expected " + TimerCommands.TRIGGER_TIMER + " command but found " + command.type());
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
        return event.payload().getLong(TIMER_ID_OFFSET);
    }
    public static int timerType(final Event event) {
        return event.payload().getInt(TIMER_TYPE_OFFSET);
    }
    public static int timerRepetition(final Event event) {
        return event.payload().getInt(TIMER_REPETITION_OFFSET);
    }
    public static long timerTimeout(final Event event) {
        return event.payload().getLong(TIMER_TIMEOUT_OFFSET);
    }

    public static boolean isTimerEvent(final Event event) {
        switch (event.type()) {
            case TIMER_EXPIRED://fallthrough
            case TIMER_FIRED://fallthrough
            case TIMER_STARTED://fallthrough
            case TIMER_STOPPED://fallthrough
                return true;
            default:
                return false;
        }
    }
}
