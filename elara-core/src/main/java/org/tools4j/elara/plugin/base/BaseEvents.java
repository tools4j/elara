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
package org.tools4j.elara.plugin.base;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.FlyweightEvent;

import static org.tools4j.elara.flyweight.FrameType.EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.NIL_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.ROLLBACK_EVENT_TYPE;

public enum BaseEvents {
    ;
    public static FlyweightEvent commit(final FlyweightEvent flyweightEvent,
                                        final MutableDirectBuffer headerBuffer,
                                        final int offset,
                                        final int sourceId,
                                        final long sourceSeq,
                                        final short index,
                                        final long time) {
        return empty(flyweightEvent, headerBuffer, offset, NIL_EVENT_TYPE, sourceId, sourceSeq, index, true,
                EventType.AUTO_COMMIT, time);
    }

    public static FlyweightEvent rollback(final FlyweightEvent flyweightEvent,
                                          final MutableDirectBuffer headerBuffer,
                                          final int offset,
                                          final int sourceId,
                                          final long sourceSeq,
                                          final short index,
                                          final long time) {
        return empty(flyweightEvent, headerBuffer, offset, ROLLBACK_EVENT_TYPE, sourceId, sourceSeq, index, true,
                EventType.ROLLBACK, time);
    }

    public static FlyweightEvent empty(final FlyweightEvent flyweightEvent,
                                       final MutableDirectBuffer headerBuffer,
                                       final int offset,
                                       final byte eventType,
                                       final int sourceId,
                                       final long sourceSeq,
                                       final short index,
                                       final boolean last,
                                       final int payloadType,
                                       final long eventTime) {
        FlyweightEvent.writeHeader(eventType, sourceId, sourceSeq, index, last, 0 /*FIXME*/, eventTime, payloadType, 0, headerBuffer, offset);
        return flyweightEvent.wrapSilently(headerBuffer, offset);
    }

    public static boolean isBaseEvent(final Event event) {
        return isBaseEvent(event.payloadType());
    }

    public static boolean isBaseEvent(final DataFrame frame) {
        final byte frameType = frame.type();;
        final int payloadType = frame.payloadType();
        return (frameType == EVENT_TYPE && payloadType == EventType.APPLICATION) ||
                (frameType == NIL_EVENT_TYPE && payloadType == EventType.AUTO_COMMIT) ||
                (frameType == ROLLBACK_EVENT_TYPE && payloadType == EventType.ROLLBACK);
    }

    public static boolean isBaseEvent(final int payloadType) {
        switch (payloadType) {
            case EventType.APPLICATION://fallthrough
            case EventType.AUTO_COMMIT://fallthrough
            case EventType.ROLLBACK://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String baseEventName(final Event event) {
        return baseEventName(event.payloadType());
    }

    public static String baseEventName(final DataFrame frame) {
        return baseEventName(frame.payloadType());
    }

    public static String baseEventName(final int eventType) {
        switch (eventType) {
            case EventType.APPLICATION:
                return "APPLICATION";
            case EventType.AUTO_COMMIT:
                return "COMMIT";
            case EventType.ROLLBACK:
                return "ROLLBACK";
            default:
                throw new IllegalArgumentException("Not a base event type: " + eventType);
        }
    }

    public static char baseEventCode(final Event event) {
        return baseEventCode(event.payloadType());
    }

    public static char baseEventCode(final DataFrame frame) {
        return baseEventCode(frame.payloadType());
    }

    public static char baseEventCode(final int eventType) {
        switch (eventType) {
            case EventType.APPLICATION:
                return 'A';
            case EventType.AUTO_COMMIT:
                return 'C';
            case EventType.ROLLBACK:
                return 'R';
            default:
                throw new IllegalArgumentException("Not a base event type: " + eventType);
        }
    }
}
