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
package org.tools4j.elara.format;

import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.Header;

public interface DataFrameFormatter extends ValueFormatter<DataFrame> {

    DataFrameFormatter DEFAULT = new DataFrameFormatter() {};

    /** Placeholder in format string for data frame itself */
    String FRAME = "{frame}";
    /** Placeholder in format string for data frame's header */
    String HEADER = "{header}";

    /** Placeholder in format string for log line no */
    String LINE = "{line}";
    /** Placeholder in format string for data frame header's input value */
    String INPUT = "{input}";
    /** Placeholder in format string for data frame header's type value */
    String TYPE = "{type}";
    /** Placeholder in format string for data frame header's sequence value */
    String SEQUENCE = "{sequence}";
    /** Placeholder in format string for data frame header's time value */
    String TIME = "{time}";
    /** Placeholder in format string for data frame header's version value */
    String VERSION = "{version}";
    /** Placeholder in format string for data frame header's index value */
    String INDEX = "{index}";
    /** Placeholder in format string for data frame header's  payload-size value */
    String PAYLOAD_SIZE = "{payload-size}";
    /** Placeholder in format string for data frame's payload value */
    String PAYLOAD = "{payload}";

    default Object line(long line, DataFrame frame) {return line;}
    default Object input(long line, DataFrame frame) {return frame.header().input();}
    default Object type(long line, DataFrame frame) {
        final Header header = frame.header();
        return header.index() >= 0 && header.type() == EventType.COMMIT ? "C" : header.type();
    }
    default Object sequence(long line, DataFrame frame) {return frame.header().sequence();}
    default Object time(long line, DataFrame frame) {return frame.header().time();}
    default Object version(long line, DataFrame frame) {return frame.header().version();}
    default Object index(long line, DataFrame frame) {return frame.header().index();}
    default Object payloadSize(long line, DataFrame frame) {return frame.header().payloadSize();}
    default Object payload(long line, DataFrame frame) {
        final int size = frame.payload().capacity();
        return size == 0 ? "(empty)" : "(" + size + " bytes)";}

    @Override
    default Object value(final String placeholder, final long line, final DataFrame frame) {
        switch (placeholder) {
            case LINE_SEPARATOR: return System.lineSeparator();
            case MESSAGE://fallthrough
            case FRAME: return frame;
            case HEADER: return frame.header();
            case LINE: return line(line, frame);
            case INPUT: return input(line, frame);
            case TYPE: return type(line, frame);
            case SEQUENCE: return sequence(line, frame);
            case TIME: return time(line, frame);
            case VERSION: return version(line, frame);
            case INDEX: return index(line, frame);
            case PAYLOAD_SIZE: return payloadSize(line, frame);
            case PAYLOAD: return payload(line, frame);
            default: return placeholder;
        }
    }
}
