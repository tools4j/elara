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
import org.tools4j.elara.flyweight.FlyweightCommand;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.DataFrameFormatter.SEQUENCE;
import static org.tools4j.elara.format.DataFrameFormatter.SOURCE;
import static org.tools4j.elara.format.DataFrameFormatter.spacer;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public enum MessagePrinters {
    ;

    public static final String VERSION_LINE     = "(elara message log format V{version}){nl}";
    public static final String GENERAL_FORMAT   = "{line}: {message}{nl}";
    public static final String PIPE_FORMAT      = "{line}: src={source}|tp={type}}|sq={sequence}|tm={time}}|vr={version}|ix={index}|sz={payload-size}{nl}";
    public static final String SHORT_FORMAT     = "{line}: src={source}, tp={type}, sq={sequence}, tm={time}, vs={version}, ix={index}, sz={payload-size}{nl}";
    public static final String LONG_FORMAT      = "{line}: source={source}, type={type}, sequence={sequence}, time={time}, version={version}, index={index}, size={payload-size}{nl}";
    public static final String COMMAND_FORMAT   = "{time} | {line} - cmd={source}:{sequence} | type={type}, payload({payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_0   = "{time} | {line} - evt={source}:{sequence}.{index} | type={type}, payload({payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_N   = "{time} | {line} - evt={source}.{sequence}.{index} | type={type}, payload({payload-size})={payload}{nl}";

    public static final MessagePrinter<Object> GENERAL = parameterized(GENERAL_FORMAT, ValueFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> PIPE = parameterized(PIPE_FORMAT, DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> SHORT = parameterized(SHORT_FORMAT, DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> LONG = parameterized(LONG_FORMAT, DataFrameFormatter.DEFAULT);

    public static final MessagePrinter<DataFrame> COMMAND = command(DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> EVENT = event(DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> FRAME = frame(DataFrameFormatter.DEFAULT);

    public static MessagePrinter<DataFrame> command(final DataFrameFormatter formatter) {
        return command(VERSION_LINE, COMMAND_FORMAT, formatter);
    }

    public static MessagePrinter<DataFrame> command(final String versionLine,
                                                    final String commandLine,
                                                    final DataFrameFormatter formatter) {
        requireNonNull(versionLine);
        requireNonNull(commandLine);
        requireNonNull(formatter);
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(versionLine + commandLine, formatter),
                parameterized(commandLine, formatter)
        );
    }

    public static MessagePrinter<DataFrame> event(final DataFrameFormatter formatter) {
        return event(VERSION_LINE, EVENT_FORMAT_0, EVENT_FORMAT_N, formatter,
                spacer(formatter, '.', SOURCE, SEQUENCE));
    }

    public static MessagePrinter<DataFrame> event(final String versionLine,
                                                  final String eventLine0,
                                                  final String eventLineN,
                                                  final DataFrameFormatter formatter0,
                                                  final DataFrameFormatter formatterN) {
        return composite((line, entryId, frame) -> {
                    if (line == 0) return 0;
                    if (frame.header().index() == 0) return 1;
                    return 2;
                },
                parameterized(versionLine + eventLine0, formatter0),
                parameterized(eventLine0, formatter0),
                parameterized(eventLineN, formatterN)
        );
    }

    public static MessagePrinter<DataFrame> frame(final DataFrameFormatter formatter) {
        return composite(
                (line, entryId, frame) -> frame.header().index() == FlyweightCommand.INDEX ? 0 : 1,
                command(formatter),
                event(formatter)
        );
    }

    public static MessagePrinter<DataFrame> frame(final String versionLine,
                                                  final String commandLine,
                                                  final String eventLine0,
                                                  final String eventLineN,
                                                  final DataFrameFormatter commandFormatter,
                                                  final DataFrameFormatter eventFormatter0,
                                                  final DataFrameFormatter eventFormatterN) {
        return composite(
                (line, entryId, frame) -> frame.header().index() == FlyweightCommand.INDEX ? 0 : 1,
                command(versionLine, commandLine, commandFormatter),
                event(versionLine, eventLine0, eventLineN, eventFormatter0, eventFormatterN)
        );
    }
}
