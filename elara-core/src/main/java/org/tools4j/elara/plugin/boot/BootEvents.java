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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.Frame;

/**
 * Boot events issued in response to boot commands.
 */
public enum BootEvents {
    ;
    /**
     * Event type signalling the start of the elara application.  The event is typically the first event after the
     * replayed events and carries the timestamp when the application was started before replaying events.
     */
    public static final int APP_INITIALISATION_STARTED = -20;
    /**
     * Event type signalling the start of the elara application.  The event is one of the first events after
     * {@link #APP_INITIALISATION_STARTED} but a few other non-replayed events may come before it due to racing.  The
     * event carries a timestamp when event replaying was complete and can hence be significantly larger than the
     * timestamp of {@link #APP_INITIALISATION_STARTED}.
     */
    public static final int APP_INITIALISATION_COMPLETED = -21;

    public static boolean isBootEvent(final Event event) {
        return isBootEvent(event.type());
    }

    public static boolean isBootEvent(final Frame frame) {
        return frame.header().index() >= 0 && isBootEvent(frame.header().type());
    }

    public static boolean isBootEvent(final int eventType) {
        switch (eventType) {
            case APP_INITIALISATION_STARTED://fallthrough
            case APP_INITIALISATION_COMPLETED://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String bootEventName(final Event event) {
        return bootEventName(event.type());
    }

    public static String bootEventName(final Frame frame) {
        return bootEventName(frame.header().type());
    }

    public static String bootEventName(final int eventType) {
        switch (eventType) {
            case APP_INITIALISATION_STARTED:
                return "APP_INITIALISATION_STARTED";
            case APP_INITIALISATION_COMPLETED:
                return "APP_INITIALISATION_COMPLETED";
            default:
                throw new IllegalArgumentException("Not a boot event type: " + eventType);
        }
    }
}
