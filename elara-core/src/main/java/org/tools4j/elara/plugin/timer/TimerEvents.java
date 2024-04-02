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

import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.FrameType;

/**
 * Timer events routed by {@link TimerCommandProcessor} and {@link TimerController}.
 */
public enum TimerEvents {
    ;
    /** Payload type for event indicating that a timer has been started.*/
    public static final int TIMER_STARTED = TimerPayloadTypes.START_TIMER;
    /** Payload type for event indicating that a timer has been cancelled.*/
    public static final int TIMER_CANCELLED = TimerPayloadTypes.CANCEL_TIMER;
    /** Payload type for event indicating that a timer has expired or triggered */
    public static final int TIMER_SIGNALLED = TimerPayloadTypes.SIGNAL_TIMER;

    public static boolean isTimerEvent(final Event event) {
        return isTimerEvent(event.payloadType());
    }

    public static boolean isTimerEvent(final DataFrame frame) {
        return FrameType.isAppRoutedEventType(frame.type()) && isTimerEvent(frame.payloadType());
    }

    public static boolean isTimerEvent(final int payloadType) {
        return TimerPayloadTypes.isTimerPayloadType(payloadType);
    }

    public static String timerEventName(final Event event) {
        return timerEventName(event.payloadType());
    }

    public static String timerEventName(final DataFrame frame) {
        return timerEventName(frame.payloadType());
    }

    public static String timerEventName(final int payloadType) {
        switch (payloadType) {
            case TIMER_STARTED:
                return "TIMER_STARTED";
            case TIMER_CANCELLED:
                return "TIMER_CANCELLED";
            case TIMER_SIGNALLED:
                return "TIMER_SIGNALLED";
            default:
                throw new IllegalArgumentException("Not a timer event type: " + payloadType);
        }
    }
}
