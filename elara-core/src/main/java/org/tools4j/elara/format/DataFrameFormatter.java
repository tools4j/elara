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

import org.tools4j.elara.flyweight.DataFrame;

import static org.tools4j.elara.flyweight.FrameType.AUTO_COMMIT_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;
import static org.tools4j.elara.flyweight.FrameType.COMMIT_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.FREQUENCY_METRICS_TYPE;
import static org.tools4j.elara.flyweight.FrameType.INTERMEDIARY_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.ROLLBACK_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.TIME_METRICS_TYPE;
import static org.tools4j.elara.format.Hex.hex;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing {@link F} elements.
 */
public interface DataFrameFormatter<F extends DataFrame> extends FrameFormatter<F> {

    /** Placeholder in format string for data frame header's source-ID value */
    String SOURCE_ID = "{source-id}";
    /** Placeholder in format string for data frame header's source sequence value */
    String SOURCE_SEQUENCE = "{source-sequence}";
    /** Placeholder in format string for data frame header's command or event time value */
    String TIME = "{time}";
    /** Placeholder in format string for data frame header's payload-type value */
    String PAYLOAD_TYPE = "{payload-type}";
    /** Placeholder in format string for data frame header's payload-size value */
    String PAYLOAD_SIZE = "{payload-size}";
    /** Placeholder in format string for data frame's payload value */
    String PAYLOAD = "{payload}";

    default Object line(long line, long entryId, F frame) {return line;}
    default Object entryId(long line, long entryId, F frame) {return entryId;}
    default Object sourceId(long line, long entryId, F frame) {return frame.sourceId();}
    default Object frameType(long line, long entryId, F frame) {
        final int type = frame.type();
        switch (type) {
            case COMMAND_TYPE:
                return "cmd";
            case INTERMEDIARY_EVENT_TYPE:
            case COMMIT_EVENT_TYPE:
            case AUTO_COMMIT_EVENT_TYPE:
                return "evt";
            case ROLLBACK_EVENT_TYPE:
                return "rbk";
            case TIME_METRICS_TYPE:
                return "tim";
            case FREQUENCY_METRICS_TYPE:
                return "frq";
            default:
                return type;
        }
    }

    default Object sourceSequence(long line, long entryId, F frame) {return frame.sourceSequence();}
    Object time(long line, long entryId, F frame);

    default Object payloadType(long line, long entryId, F frame) {
        return frame.payloadType();
    }
    default Object payloadSize(long line, long entryId, F frame) {return frame.payloadSize();}
    default Object payload(long line, long entryId, F frame) {
        final int size = frame.payload().capacity();
        return size == 0 ? "(empty)" : hex(frame.payload());
    }

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final F frame) {
        switch (placeholder) {
            case SOURCE_ID: return sourceId(entryId, entryId, frame);
            case SOURCE_SEQUENCE: return sourceSequence(entryId, entryId, frame);
            case TIME: return time(entryId, entryId, frame);
            case PAYLOAD_TYPE: return payloadType(entryId, entryId, frame);
            case PAYLOAD_SIZE: return payloadSize(entryId, entryId, frame);
            case PAYLOAD: return payload(entryId, entryId, frame);
            default: return FrameFormatter.super.value(placeholder, line, entryId, frame);
        }
    }

}
