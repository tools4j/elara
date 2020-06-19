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

import org.tools4j.elara.flyweight.DataFrame;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.DataFrameFormatter.DEFAULT;
import static org.tools4j.elara.format.DataFrameFormatter.VERSION;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public enum MessagePrinters {
    ;

    public static final String GENERAL_FORMAT   = "{line}: {message}{nl}";
    public static final String PIPE_FORMAT      = "{line}: in={source}|tp={type}}|sq={sequence}|tm={time}}|vs={version}|ix={index}|sz={payload-size}{nl}";
    public static final String SHORT_FORMAT     = "{line}: in={source}, tp={type}, sq={sequence}, tm={time}, vs={version}, ix={index}, sz={payload-size}{nl}";
    public static final String LONG_FORMAT      = "{line}: source={source}, type={type}, sequence={sequence}, time={time}, version={version}, index={index}, size={payload-size}{nl}";
    public static final String COMMAND_FORMAT   = "{line}: cmd={source}:{sequence} | type={type}, payload={payload} at {time}{version}{nl}";
    public static final String EVENT_FORMAT_CMD = "{line}: evt={source}:{sequence}.{index} | type={type}, payload={payload} at {time}{version}{nl}";
    public static final String EVENT_FORMAT_EVT = "{line}: evt={src-spc}.{seq-spc}.{index} | type={type}, payload={payload}{nl}";

    public static final MessagePrinter<Object> GENERAL = parameterized(GENERAL_FORMAT, ValueFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> PIPE = parameterized(PIPE_FORMAT, DEFAULT);
    public static final MessagePrinter<DataFrame> SHORT = parameterized(SHORT_FORMAT, DEFAULT);
    public static final MessagePrinter<DataFrame> LONG = parameterized(LONG_FORMAT, DEFAULT);

    public static final MessagePrinter<DataFrame> COMMAND = command(DEFAULT);
    public static MessagePrinter<DataFrame> command(final DataFrameFormatter formatter) {
        requireNonNull(formatter);
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(COMMAND_FORMAT.replace(VERSION, " (V {version})"), formatter),
                parameterized(COMMAND_FORMAT.replace(VERSION, ""), formatter)
        );
    }

    public static final MessagePrinter<DataFrame> EVENT = event(DEFAULT);
    public static MessagePrinter<DataFrame> event(final DataFrameFormatter formatter) {
        requireNonNull(formatter);
        final DataFrameFormatter sourceSeqSpacer = new DataFrameFormatter() {
            @Override
            public Object value(final String placeholder, final long line, final long entryId, final DataFrame frame) {
                switch (placeholder) {
                    case "{src-spc}":
                        return String.valueOf(formatter.source(line, entryId, frame)).replaceAll(".", ".");
                    case "{seq-spc}":
                        return String.valueOf(formatter.sequence(line, entryId, frame)).replaceAll(".", ".");
                    default:
                        return formatter.value(placeholder, line, entryId, frame);
                }
            }
        };
        return composite(
                (line, entryId, frame) -> {
                    if (line == 0) return 0;
                    if (frame.header().index() == 0) return 1;
                    return 2;
                },
                parameterized(EVENT_FORMAT_CMD.replace(VERSION, " (V {version})"), sourceSeqSpacer),
                parameterized(EVENT_FORMAT_CMD, sourceSeqSpacer),
                parameterized(EVENT_FORMAT_EVT, sourceSeqSpacer)
        );
    }

    public static final MessagePrinter<DataFrame> FRAME = frame(DEFAULT);
    public static MessagePrinter<DataFrame> frame(final DataFrameFormatter formatter) {
        requireNonNull(formatter);
        return composite((line, entryId, frame) -> frame.header().index() < 0 ? 0 : 1, command(formatter), event(formatter));
    }
}
