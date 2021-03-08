/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.plugin.metrics.MetricsLogEntry;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry.Type;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.DataFrameFormatter.SEQUENCE;
import static org.tools4j.elara.format.DataFrameFormatter.SOURCE;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public enum MessagePrinters {
    ;

    public static final String VERSION_LINE             = "(elara message log format V{version}){nl}";
    public static final String GENERAL_FORMAT           = "{line}: {message}{nl}";
    public static final String PIPE_FORMAT              = "{line}: src={source}|tp={type}}|sq={sequence}|tm={time}}|vr={version}|ix={index}|sz={payload-size}{nl}";
    public static final String SHORT_FORMAT             = "{line}: src={source}, tp={type}, sq={sequence}, tm={time}, vs={version}, ix={index}, sz={payload-size}{nl}";
    public static final String LONG_FORMAT              = "{line}: source={source}, type={type}, sequence={sequence}, time={time}, version={version}, index={index}, size={payload-size}{nl}";
    public static final String COMMAND_FORMAT           = "{time} | {line} - cmd={source}:{sequence} | type={type}, payload({payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_0           = "{time} | {line} - evt={source}:{sequence}.{index} | type={type}, payload({payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_N           = "{time} | {line} - evt={source}.{sequence}.{index} | type={type}, payload({payload-size})={payload}{nl}";

    public static final String METRICS_VERSION_LINE     = "(elara metrics log format V{version}){nl}";
    public static final String METRICS_COMMAND_FORMAT   = "{time} | {line} - {type} cmd={source}:{sequence}   | {metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT_0   = "{time} | {line} - {type} evt={source}:{sequence}.{index} | {metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT_N   = "{time} | {line} - {type} evt={source}.{sequence}.{index} | {metrics-values}{nl}";
    public static final String METRICS_OUTPUT_FORMAT_0  = METRICS_EVENT_FORMAT_0;
    public static final String METRICS_OUTPUT_FORMAT_N  = METRICS_EVENT_FORMAT_N;
    public static final String METRICS_FREQUENCY_FORMAT = "{time} | {line} - {type} rep={repetition}, intvl={interval} | {metrics-values}{nl}";
    public static final String METRICS_VALUE_FORMAT_0   = "{metric-name}={metric-value}";
    public static final String METRICS_VALUE_FORMAT_N   = ", {metric-name}={metric-value}";

    public static final MessagePrinter<Object> GENERAL = parameterized(GENERAL_FORMAT, ValueFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> PIPE = parameterized(PIPE_FORMAT, DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> SHORT = parameterized(SHORT_FORMAT, DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> LONG = parameterized(LONG_FORMAT, DataFrameFormatter.DEFAULT);

    public static final MessagePrinter<DataFrame> COMMAND = command(DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> EVENT = event(DataFrameFormatter.DEFAULT);
    public static final MessagePrinter<DataFrame> FRAME = frame(DataFrameFormatter.DEFAULT);

    public static final MessagePrinter<MetricsLogEntry> METRICS = metrics(MetricsFormatter.DEFAULT);
    public static final MessagePrinter<MetricsLogEntry> LATENCIES = metrics(LatencyFormatter.DEFAULT);

    public static MessagePrinter<DataFrame> command(final DataFrameFormatter formatter) {
        requireNonNull(formatter);
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(VERSION_LINE + COMMAND_FORMAT, formatter),
                parameterized(COMMAND_FORMAT, formatter)
        );
    }

    public static MessagePrinter<DataFrame> event(final DataFrameFormatter formatter) {
        final ValueFormatter<DataFrame> formatter0 = formatter;
        final ValueFormatter<DataFrame> formatterN = Spacer.spacer(formatter, '.', SOURCE, SEQUENCE);
        return composite((line, entryId, frame) -> {
                    if (line == 0) return 0;
                    if (frame.header().index() == 0) return 1;
                    return 2;
                },
                parameterized(VERSION_LINE + EVENT_FORMAT_0, formatter0),
                parameterized(EVENT_FORMAT_0, formatter0),
                parameterized(EVENT_FORMAT_N, formatterN)
        );
    }

    public static MessagePrinter<DataFrame> frame(final DataFrameFormatter formatter) {
        return composite(
                (line, entryId, frame) -> frame.header().index() == FlyweightCommand.INDEX ? 0 : 1,
                command(formatter),
                event(formatter)
        );
    }

    public static MessagePrinter<MetricsLogEntry> timeMetrics(final MetricsFormatter formatter) {
        final ValueFormatter<MetricsLogEntry> formatter0 = formatter;
        final ValueFormatter<MetricsLogEntry> formatterN = Spacer.spacer(formatter, '.', SOURCE, SEQUENCE);
        return composite((line, entryId, entry) -> {
                    final Target target = entry.target();
                    switch (target) {
                        case COMMAND:
                            return line == 0 ? 0 : 1;
                        case EVENT:
                            return line == 0 ? 2 : entry.index() == 0 ? 3 : 4;
                        case OUTPUT:
                            return line == 0 ? 5 : entry.index() == 0 ? 6 : 7;
                        default:
                            throw new IllegalArgumentException("Invalid target: " + target);
                    }
                },
                parameterized(METRICS_VERSION_LINE + METRICS_COMMAND_FORMAT, formatter0),
                parameterized(METRICS_COMMAND_FORMAT, formatter0),
                parameterized(METRICS_VERSION_LINE + METRICS_EVENT_FORMAT_0, formatter0),
                parameterized(METRICS_EVENT_FORMAT_0, formatter0),
                parameterized(METRICS_EVENT_FORMAT_N, formatterN),
                parameterized(METRICS_VERSION_LINE + METRICS_OUTPUT_FORMAT_0, formatter0),
                parameterized(METRICS_OUTPUT_FORMAT_0, formatter0),
                parameterized(METRICS_OUTPUT_FORMAT_N, formatterN)
        );
    }

    public static MessagePrinter<MetricsLogEntry> frequencyMetrics(final MetricsFormatter formatter) {
        requireNonNull(formatter);
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(METRICS_VERSION_LINE + METRICS_FREQUENCY_FORMAT, formatter),
                parameterized(METRICS_FREQUENCY_FORMAT, formatter)
        );
    }
    public static MessagePrinter<MetricsLogEntry> metrics(final MetricsFormatter formatter) {
        return composite(
                (line, entryId, emtry) -> emtry.type() == Type.TIME ? 0 : 1,
                timeMetrics(formatter),
                frequencyMetrics(formatter)
        );
    }

}
