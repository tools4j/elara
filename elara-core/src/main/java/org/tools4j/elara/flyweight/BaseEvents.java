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
package org.tools4j.elara.flyweight;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.Event;

import static org.tools4j.elara.flyweight.FrameType.AUTO_COMMIT_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.ROLLBACK_EVENT_TYPE;

public enum BaseEvents {
    ;
    /**
     * Payload type used for implicit auto-commit event routed if the application does not route any events.
     *
     * @see EventType#AUTO_COMMIT
     * @see FrameType#AUTO_COMMIT_EVENT_TYPE
     */
    public static final int AUTO_COMMIT = -1;
    /**
     * Payload type used for rollback event appended if a corrupted event file was detected with an unfinished command.
     *
     * @see EventType#ROLLBACK
     * @see FrameType#ROLLBACK_EVENT_TYPE
     */
    public static final int ROLLBACK = -2;
    public static FlyweightEvent commit(final FlyweightEvent flyweightEvent,
                                        final MutableDirectBuffer headerBuffer,
                                        final int offset,
                                        final int sourceId,
                                        final long sourceSeq,
                                        final short index,
                                        final long eventSeq,
                                        final long eventTime) {
        return empty(flyweightEvent, headerBuffer, offset, EventType.AUTO_COMMIT, sourceId, sourceSeq, index, eventSeq,
                AUTO_COMMIT, eventTime);
    }

    public static FlyweightEvent rollback(final FlyweightEvent flyweightEvent,
                                          final MutableDirectBuffer headerBuffer,
                                          final int offset,
                                          final int sourceId,
                                          final long sourceSeq,
                                          final short index,
                                          final long eventSeq,
                                          final long eventTime) {
        return empty(flyweightEvent, headerBuffer, offset, EventType.ROLLBACK, sourceId, sourceSeq, index, eventSeq,
                ROLLBACK, eventTime);
    }

    public static FlyweightEvent empty(final FlyweightEvent flyweightEvent,
                                       final MutableDirectBuffer headerBuffer,
                                       final int offset,
                                       final EventType eventType,
                                       final int sourceId,
                                       final long sourceSeq,
                                       final short index,
                                       final long eventSeq,
                                       final int payloadType,
                                       final long eventTime) {
        FlyweightEvent.writeHeader(eventType, sourceId, sourceSeq, index, eventSeq, eventTime, payloadType, 0, headerBuffer, offset);
        return flyweightEvent.wrapSilently(headerBuffer, offset);
    }

    public static boolean isBaseEvent(final Event event) {
        return isBaseEvent(event.payloadType());
    }

    public static boolean isBaseEvent(final DataFrame frame) {
        final byte frameType = frame.type();;
        final int payloadType = frame.payloadType();
        return (frameType == AUTO_COMMIT_EVENT_TYPE && payloadType == AUTO_COMMIT) ||
                (frameType == ROLLBACK_EVENT_TYPE && payloadType == ROLLBACK);
    }

    public static boolean isBaseEvent(final int payloadType) {
        switch (payloadType) {
            case AUTO_COMMIT://fallthrough
            case ROLLBACK://fallthrough
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
            case AUTO_COMMIT:
                return "AUTO_COMMIT";
            case ROLLBACK:
                return "ROLLBACK";
            default:
                throw new IllegalArgumentException("Not a base event type: " + eventType);
        }
    }
}
