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
package org.tools4j.elara.format;

import org.tools4j.elara.flyweight.Frame;

import java.util.concurrent.TimeUnit;

import static org.tools4j.elara.flyweight.FrameType.AUTO_COMMIT_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;
import static org.tools4j.elara.flyweight.FrameType.COMMIT_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.FREQUENCY_METRICS_TYPE;
import static org.tools4j.elara.flyweight.FrameType.INTERMEDIARY_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.ROLLBACK_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.TIME_METRICS_TYPE;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing a {@link Frame}.
 */
public interface FrameFormatter<F extends Frame> extends ValueFormatter<F> {

    /** Placeholder in format string for data frame itself */
    String FRAME = "{frame}";
    /** Placeholder in format string for data frame's header */
    String HEADER = "{header}";
    /** Placeholder in format string for data frame header's version value */
    String VERSION = "{version}";
    /** Placeholder in format string for data frame header's type value */
    String FRAME_TYPE = "{frame-type}";
    /** Placeholder in format string for choice value available when printing frequency metrics */
    String TIME_UNIT = "{time-unit}";

    default Object line(long line, long entryId, F frame) {return line;}
    default Object entryId(long line, long entryId, F frame) {return entryId;}
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
    default Object version(long line,long entryId,  F frame) {return frame.header().version();}
    default Object timeUnit(long line, long entryId, F frame) {
        final TimeUnit timeUnit = timeFormatter().timeUnit();
        if (timeUnit != null) {
            switch (timeUnit) {
                case NANOSECONDS: return "ns";
                case MICROSECONDS: return "us";
                case MILLISECONDS: return "ms";
                case SECONDS: return "s";
                case MINUTES: return "m";
                case HOURS: return "h";
                case DAYS: return "d";
            }
        }
        return "";
    }

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final F frame) {
        switch (placeholder) {
            case LINE_SEPARATOR: return System.lineSeparator();
            case MESSAGE://fallthrough
            case FRAME: return frame;
            case HEADER: return frame.header();
            case VERSION: return version(entryId, entryId, frame);
            case FRAME_TYPE: return frameType(entryId, entryId, frame);
            case LINE: return line(line, entryId, frame);
            case ENTRY_ID: return entryId(line, entryId, frame);
            case TIME_UNIT: return timeUnit(line, entryId, frame);
            default: return placeholder;
        }
    }

    default TimeFormatter timeFormatter() {
        return TimeFormatter.DEFAULT;
    }
}
