/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.command.CommandType;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.Header;

import java.time.Instant;

import static org.tools4j.elara.format.Hex.hex;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing {@link DataFrame} elements.
 */
public interface DataFrameFormatter extends ValueFormatter<DataFrame> {

    DataFrameFormatter DEFAULT = new DataFrameFormatter() {};

    /** Placeholder in format string for data frame itself */
    String FRAME = "{frame}";
    /** Placeholder in format string for data frame's header */
    String HEADER = "{header}";

    /** Placeholder in format string for data frame header's source value */
    String SOURCE = "{source}";
    /** Placeholder in format string for data frame header's type value */
    String TYPE = "{type}";
    /** Placeholder in format string for data frame header's sequence value */
    String SEQUENCE = "{sequence}";
    /** Placeholder in format string for data frame header's time value */
    String TIME = "{time}";
    /** Placeholder in format string for data frame header's version value */
    String VERSION = "{version}";
    /** Placeholder in format string for data frame header's flags value */
    String FLAGS = "{flags}";
    /** Placeholder in format string for data frame header's index value */
    String INDEX = "{index}";
    /** Placeholder in format string for data frame header's  payload-size value */
    String PAYLOAD_SIZE = "{payload-size}";
    /** Placeholder in format string for data frame's payload value */
    String PAYLOAD = "{payload}";

    default Object line(long line, long entryId, DataFrame frame) {return line;}
    default Object entryId(long line, long entryId, DataFrame frame) {return entryId;}
    default Object source(long line, long entryId, DataFrame frame) {return frame.header().source();}
    default Object type(long line, long entryId, DataFrame frame) {
        final Header header = frame.header();
        final int type = header.type();
        if (header.index() >= 0) {
            //event
            switch (type) {
                case EventType.APPLICATION:
                    return "A";
                case EventType.COMMIT:
                    return "C";
                case EventType.ROLLBACK:
                    return "R";
            }
        }
        if (header.index() == FlyweightCommand.INDEX) {
            //command
            if (type == CommandType.APPLICATION) {
                return "A";
            }
        }
        return type;
    }
    default Object sequence(long line, long entryId, DataFrame frame) {return frame.header().sequence();}
    default Object time(long line, long entryId, DataFrame frame) {return Instant.ofEpochMilli(frame.header().time());}
    default Object version(long line,long entryId,  DataFrame frame) {return frame.header().version();}
    default Object flags(long line,long entryId,  DataFrame frame) {return Flags.toString(frame.header().flags());}
    default Object index(long line, long entryId, DataFrame frame) {return frame.header().index();}
    default Object payloadSize(long line, long entryId, DataFrame frame) {return frame.header().payloadSize();}
    default Object payload(long line, long entryId, DataFrame frame) {
        final int size = frame.payload().capacity();
        return size == 0 ? "(empty)" : hex(frame.payload());}

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final DataFrame frame) {
        switch (placeholder) {
            case LINE_SEPARATOR: return System.lineSeparator();
            case MESSAGE://fallthrough
            case FRAME: return frame;
            case HEADER: return frame.header();
            case LINE: return line(line, entryId, frame);
            case ENTRY_ID: return entryId(line, entryId, frame);
            case SOURCE: return source(entryId, entryId, frame);
            case TYPE: return type(entryId, entryId, frame);
            case SEQUENCE: return sequence(entryId, entryId, frame);
            case TIME: return time(entryId, entryId, frame);
            case VERSION: return version(entryId, entryId, frame);
            case FLAGS: return flags(entryId, entryId, frame);
            case INDEX: return index(entryId, entryId, frame);
            case PAYLOAD_SIZE: return payloadSize(entryId, entryId, frame);
            case PAYLOAD: return payload(entryId, entryId, frame);
            default: return placeholder;
        }
    }

}
