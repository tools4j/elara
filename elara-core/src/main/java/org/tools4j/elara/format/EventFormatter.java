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
package org.tools4j.elara.format;

import org.tools4j.elara.flyweight.BaseEvents;
import org.tools4j.elara.flyweight.EventFrame;
import org.tools4j.elara.flyweight.PayloadType;

import static java.util.Objects.requireNonNull;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing {@link EventFrame} elements.
 */
public interface EventFormatter extends DataFrameFormatter<EventFrame> {

    EventFormatter DEFAULT = new EventFormatter() {};

    static EventFormatter create(final TimeFormatter timeFormatter) {
        requireNonNull(timeFormatter);
        return new EventFormatter() {
            @Override
            public TimeFormatter timeFormatter() {
                return timeFormatter;
            }
        };
    }

    /** Placeholder in format string for data frame header's event sequence value */
    String EVENT_SEQUENCE = "{event-sequence}";
    /** Placeholder in format string for event frame header's event index value */
    String EVENT_INDEX = "{event-index}";

    default Object time(long line, long entryId, EventFrame frame) {
        return timeFormatter().formatDateTime(frame.eventTime());
    }
    default Object eventSequence(long line, long entryId, EventFrame frame) {
        return frame.eventSequence();
    }
    default Object eventIndex(long line, long entryId, EventFrame frame) {
        return frame.eventIndex();
    }
    default Object payloadType(long line, long entryId, EventFrame frame) {
        final int payloadType = frame.payloadType();
        switch (payloadType) {
            case PayloadType.DEFAULT:
                return "D";
            case BaseEvents.AUTO_COMMIT:
                return "C";
            case BaseEvents.ROLLBACK:
                return "R";
            default:
                return payloadType;
        }
    }
    @Override
    default Object value(final String placeholder, final long line, final long entryId, final EventFrame frame) {
        switch (placeholder) {
            case EVENT_SEQUENCE: return eventSequence(entryId, entryId, frame);
            case EVENT_INDEX: return eventIndex(entryId, entryId, frame);
            default: return DataFrameFormatter.super.value(placeholder, line, entryId, frame);
        }
    }
}
