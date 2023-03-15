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

import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;
import static org.tools4j.elara.format.DataFrameFormatter.SEQUENCE;
import static org.tools4j.elara.format.DataFrameFormatter.SOURCE;
import static org.tools4j.elara.format.HistogramFormatter.CaptureResult.PRINT;
import static org.tools4j.elara.format.MessagePrinter.NOOP;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.iterationToken;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public class DefaultMessagePrinters implements MessagePrinters {

    public static final String ITEM_SEPARATOR           = ", ";
    public static final String VERSION_LINE             = "(elara message store format V{version}){nl}";
    public static final String COMMAND_FORMAT           = "{time} | {line} - cmd={source}:{sequence} | type={type}, payload({payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_0           = "{time} | {line} - evt={source}:{sequence}.{index} | type={type}, payload({payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_N           = "{time} | {line} - evt={source}.{sequence}.{index} | type={type}, payload({payload-size})={payload}{nl}";

    public static final String METRICS_VERSION_LINE     = "(elara metrics store format V{version}){nl}";
    public static final String METRICS_COMMAND_FORMAT   = "{time} | {line} - {type} cmd={source}:{sequence}   | {metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT_0   = "{time} | {line} - {type} evt={source}:{sequence}.{index} | {metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT_N   = "{time} | {line} - {type} evt={source}.{sequence}.{index} | {metrics-values}{nl}";
    public static final String METRICS_OUTPUT_FORMAT_0  = METRICS_EVENT_FORMAT_0;
    public static final String METRICS_OUTPUT_FORMAT_N  = METRICS_EVENT_FORMAT_N;
    public static final String METRICS_FREQUENCY_FORMAT = "{time} | {line} - {type} rep={repetition}, intvl={interval}{time-unit} | {metrics-values}{nl}";
    public static final String METRICS_NAME_FORMAT      = "{sep}{metric-name}";
    public static final String METRICS_VALUE_FORMAT     = "{sep}{metric-name}={metric-value}";
    public static final String LATENCY_VALUE_FORMAT     = "{sep}{metric-name}={metric-value}{time-unit}";
    public static final String HISTOGRAM_FORMAT         = "{histogram-values}";
    public static final String HISTOGRAM_VALUE_FORMAT   = "{time} | {line} - {type} rep={repetition}, intvl={interval} | {metric-name} - n={value-count}, {bucket-values}{nl}";
    public static final String HISTOGRAM_BUCKET_VALUE   = "{sep}{bucket-name}={bucket-value}";
    private final DataFrameFormatter dataFrameFormatter;
    private final MetricsFormatter baseMetricsFormatter;
    private final LatencyFormatter baseLatencyFormatter;
    private final HistogramFormatter baseHistogramFormatter;

    public DefaultMessagePrinters() {
        this(DataFrameFormatter.DEFAULT, MetricsFormatter.DEFAULT, LatencyFormatter.DEFAULT, HistogramFormatter.DEFAULT);
    }

    public DefaultMessagePrinters(final TimeFormatter timeFormatter, final long interval) {
        this(DataFrameFormatter.create(timeFormatter), MetricsFormatter.create(timeFormatter),
                LatencyFormatter.create(timeFormatter), HistogramFormatter.create(timeFormatter, interval));
    }

    public DefaultMessagePrinters(final DataFrameFormatter dataFrameFormatter,
                                  final MetricsFormatter metricsFormatter,
                                  final LatencyFormatter latencyFormatter,
                                  final HistogramFormatter histogramFormatter) {
        this.dataFrameFormatter = requireNonNull(dataFrameFormatter);
        this.baseMetricsFormatter = requireNonNull(metricsFormatter);
        this.baseLatencyFormatter = requireNonNull(latencyFormatter);
        this.baseHistogramFormatter = requireNonNull(histogramFormatter);
    }

    @Override
    public MessagePrinter<DataFrame> command() {
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(VERSION_LINE + COMMAND_FORMAT, dataFrameFormatter),
                parameterized(COMMAND_FORMAT, dataFrameFormatter)
        );
    }

    @Override
    public MessagePrinter<DataFrame> event() {
        final ValueFormatter<DataFrame> formatter0 = dataFrameFormatter;
        final ValueFormatter<DataFrame> formatterN = Spacer.spacer(formatter0, '.', SOURCE, SEQUENCE);
        return composite((line, entryId, frame) -> {
                    if (line == 0) return 0;
                    if (frame.type() == COMMAND_TYPE) return 1;
                    return 2;
                },
                parameterized(VERSION_LINE + EVENT_FORMAT_0, formatter0),
                parameterized(EVENT_FORMAT_0, formatter0),
                parameterized(EVENT_FORMAT_N, formatterN)
        );
    }

    protected ValueFormatter<MetricsStoreEntry> metricsFormatter(final MetricsFormatter baseFormatter,
                                                                 final String metricsValueFormat) {
        return baseFormatter
                .then(iterationToken("{metrics-names}", METRICS_NAME_FORMAT, ITEM_SEPARATOR,
                        baseFormatter.metricNameFormatter(), baseFormatter::metricValues, baseFormatter))
                .then(iterationToken("{metrics-values}", metricsValueFormat, ITEM_SEPARATOR,
                        baseFormatter.metricValueFormatter(), baseFormatter::metricValues, baseFormatter));
    }

    protected ValueFormatter<MetricsStoreEntry> histogramFormatter(final HistogramFormatter baseFormatter) {
        return metricsFormatter(baseFormatter, LATENCY_VALUE_FORMAT)
                .then(iterationToken("{metrics-names}", METRICS_NAME_FORMAT, ITEM_SEPARATOR,
                        baseFormatter.metricNameFormatter(), baseFormatter::metricValues, baseFormatter))
                .then(iterationToken("{histogram-values}", HISTOGRAM_VALUE_FORMAT, "",
                        baseFormatter.histogramValuesFormatter(HISTOGRAM_BUCKET_VALUE, ITEM_SEPARATOR),
                        baseFormatter::histogramValues, baseFormatter));

    }

    protected MessagePrinter<MetricsStoreEntry> timeMetrics(final ValueFormatter<MetricsStoreEntry> formatter) {
        final ValueFormatter<MetricsStoreEntry> formatter0 = requireNonNull(formatter);
        final ValueFormatter<MetricsStoreEntry> formatterN = Spacer.spacer(formatter0, '.', SOURCE, SEQUENCE);
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

    @Override
    public MessagePrinter<MetricsStoreEntry> frequencyMetrics() {
        final ValueFormatter<MetricsStoreEntry> formatter = metricsFormatter(baseMetricsFormatter, METRICS_VALUE_FORMAT);
        return composite(
                (line, entryId, entry) -> line == 0 ? 0 : 1,
                parameterized(METRICS_VERSION_LINE + METRICS_FREQUENCY_FORMAT, formatter),
                parameterized(METRICS_FREQUENCY_FORMAT, formatter)
        );
    }

    @Override
    public MessagePrinter<MetricsStoreEntry> timeMetrics() {
        return timeMetrics(metricsFormatter(baseMetricsFormatter, METRICS_VALUE_FORMAT));
    }

    @Override
    public MessagePrinter<MetricsStoreEntry> latencyMetrics() {
        return timeMetrics(metricsFormatter(baseLatencyFormatter, LATENCY_VALUE_FORMAT));
    }

    @Override
    public MessagePrinter<MetricsStoreEntry> latencyHistogram() {
        final ValueFormatter<MetricsStoreEntry> formatter = histogramFormatter(baseHistogramFormatter);
        return composite(
                (line, entryId, entry) -> {
                    if (line == 0) {
                        baseHistogramFormatter.latencyHistograms.remove();
                    }
                    final boolean print = baseHistogramFormatter.capture(line, entryId, entry) == PRINT;
                    return line == 0 ? (print ? 1 : 0) : (print ? 3 : 2);
                },
                parameterized(METRICS_VERSION_LINE, formatter),
                parameterized(METRICS_VERSION_LINE + HISTOGRAM_FORMAT, formatter),
                NOOP,
                parameterized(HISTOGRAM_FORMAT, formatter)
        );
    }

}
