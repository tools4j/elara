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
package org.tools4j.elara.plugin.base;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.flyweight.Frame;

public enum BaseEvents {
    ;
    private static final DirectBuffer EMPTY_BUFFER = new UnsafeBuffer(0, 0);

    public static FlyweightEvent commit(final FlyweightEvent flyweightEvent,
                                        final MutableDirectBuffer headerBuffer,
                                        final int offset,
                                        final int source,
                                        final long sequence,
                                        final short index,
                                        final long time) {
        return empty(flyweightEvent, headerBuffer, offset, source, sequence, index, EventType.COMMIT, time,
                Flags.COMMIT);
    }

    public static FlyweightEvent rollback(final FlyweightEvent flyweightEvent,
                                          final MutableDirectBuffer headerBuffer,
                                          final int offset,
                                          final int source,
                                          final long sequence,
                                          final short index,
                                          final long time) {
        return empty(flyweightEvent, headerBuffer, offset, source, sequence, index, EventType.ROLLBACK, time,
                Flags.ROLLBACK);
    }

    public static FlyweightEvent empty(final FlyweightEvent flyweightEvent,
                                        final MutableDirectBuffer headerBuffer,
                                        final int offset,
                                        final int source,
                                        final long sequence,
                                        final short index,
                                        final int type,
                                        final long time,
                                        final byte flags) {
        return flyweightEvent.init(headerBuffer, offset, source, sequence, index, type, time, flags,
                EMPTY_BUFFER, 0, 0);
    }

    public static boolean isBaseEvent(final Event event) {
        return isBaseEventType(event.type());
    }

    public static boolean isBaseEvent(final Frame frame) {
        return frame.header().index() >= 0 && isBaseEventType(frame.header().type());
    }

    public static boolean isBaseEventType(final int eventType) {
        switch (eventType) {
            case EventType.APPLICATION://fallthrough
            case EventType.COMMIT://fallthrough
            case EventType.ROLLBACK://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String baseEventName(final Event event) {
        return baseEventName(event.type());
    }

    public static String baseEventName(final Frame frame) {
        return baseEventName(frame.header().type());
    }

    public static String baseEventName(final int eventType) {
        switch (eventType) {
            case EventType.APPLICATION:
                return "APPLICATION";
            case EventType.COMMIT:
                return "COMMIT";
            case EventType.ROLLBACK:
                return "ROLLBACK";
            default:
                throw new IllegalArgumentException("Not a base event type: " + eventType);
        }
    }

    public static char baseEventCode(final Event event) {
        return baseEventCode(event.type());
    }

    public static char baseEventCode(final Frame frame) {
        return baseEventCode(frame.header().type());
    }

    public static char baseEventCode(final int eventType) {
        switch (eventType) {
            case EventType.APPLICATION:
                return 'A';
            case EventType.COMMIT:
                return 'C';
            case EventType.ROLLBACK:
                return 'R';
            default:
                throw new IllegalArgumentException("Not a base event type: " + eventType);
        }
    }
}
