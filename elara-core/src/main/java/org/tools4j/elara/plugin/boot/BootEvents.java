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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.FrameType;

/**
 * Boot events issued in response to boot commands.
 */
public enum BootEvents {
    ;
    /**
     * Event type signalling the start of the elara application.  The event is typically the first event after the
     * replayed events.
     */
    public static final int BOOT_APP_STARTED = -20;

    public static boolean isBootEvent(final Event event) {
        return isBootEvent(event.payloadType());
    }

    public static boolean isBootEvent(final DataFrame frame) {
        return FrameType.isAppRoutedEventType(frame.type()) && isBootEvent(frame.payloadType());
    }

    public static boolean isBootEvent(final int payloadType) {
        return payloadType == BOOT_APP_STARTED;
    }

    public static String bootEventName(final Event event) {
        return bootEventName(event.payloadType());
    }

    public static String bootEventName(final DataFrame frame) {
        return bootEventName(frame.payloadType());
    }

    public static String bootEventName(final int payloadType) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payloadType) {
            case BOOT_APP_STARTED:
                return "BOOT_APP_STARTED";
            default:
                throw new IllegalArgumentException("Not a boot event type: " + payloadType);
        }
    }
}
