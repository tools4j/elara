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
import org.tools4j.elara.format.HistogramFormatter.HistogramValues;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.HistogramFormatter.CaptureResult.PRINT;
import static org.tools4j.elara.format.MessagePrinter.NOOP;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.iterationToken;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public class CsvMessagePrinters implements MessagePrinters {

    public static final String DELIMITER                = ",";
    public static final String HEADER_LINE              = "Time,Seq,Type,Source-ID,Source-Seq,Index,Payload-Type,Payload-Size,Payload{nl}";
    public static final String COMMAND_FORMAT           = "{time},{line},CMD,{source},{sequence},,{type},{payload-size},{payload}{nl}";
    public static final String EVENT_FORMAT             = "{time},{line},EVT,{source},{sequence},{index},{type},{payload-size},{payload}{nl}";

    public static final String METRICS_HEADER           = "Time,Seq,Type,Source-ID,Source-Seq,Index,Interval,Unit,{metrics-names}{nl}";
    public static final String METRICS_COMMAND_FORMAT   = "{time},{line},{type},{source},{sequence},,,,{time-unit},{metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT     = "{time},{line},{type},{source},{sequence},{index},,,{time-unit},{metrics-values}{nl}";
    public static final String METRICS_OUTPUT_FORMAT    = METRICS_EVENT_FORMAT;
    public static final String METRICS_FREQUENCY_FORMAT = "{time},{line},{type},,,{repetition},{interval},{time-unit},{metrics-values}{nl}";
    public static final String METRICS_NAME_FORMAT      = "{sep}{metric-name}";
    public static final String METRICS_VALUE_FORMAT     = "{sep}{metric-value}";
    public static final String HISTOGRAM_HEADER         = "Time,Seq,Type,Source-ID,Source-Seq,Index,Interval,Unit,Metric,Count,{bucket-names}{nl}";
    public static final String HISTOGRAM_FORMAT         = "{histogram-values}";
    public static final String HISTOGRAM_VALUE_FORMAT   = "{time},{line},{type},,,{repetition},{interval},{time-unit},{metric-name},{value-count},{bucket-values}{nl}";
    public static final String HISTOGRAM_BUCKET_NAME    = "{sep}{bucket-name}";
    public static final String HISTOGRAM_BUCKET_VALUE   = "{sep}{bucket-value}";

    private final DataFrameFormatter dataFrameFormatter;
    private final MetricsFormatter baseMetricsFormatter;
    private final LatencyFormatter baseLatencyFormatter;
    private final HistogramFormatter baseHistogramFormatter;

    public CsvMessagePrinters() {
        this(DataFrameFormatter.DEFAULT, MetricsFormatter.DEFAULT, LatencyFormatter.DEFAULT, HistogramFormatter.DEFAULT);
    }

    public CsvMessagePrinters(final TimeFormatter timeFormatter, final long interval) {
        this(DataFrameFormatter.create(timeFormatter), MetricsFormatter.create(timeFormatter),
                LatencyFormatter.create(timeFormatter), HistogramFormatter.create(timeFormatter, interval));
    }

    public CsvMessagePrinters(final DataFrameFormatter dataFrameFormatter,
                              final MetricsFormatter metricsFormatter,
                              final LatencyFormatter latencyFormatter,
                              final HistogramFormatter histogramFormatter) {
        this.dataFrameFormatter = requireNonNull(dataFrameFormatter);
        this.baseMetricsFormatter = requireNonNull(metricsFormatter);
        this.baseLatencyFormatter = requireNonNull(latencyFormatter);
        this.baseHistogramFormatter = requireNonNull(histogramFormatter);
    }

    protected ValueFormatter<MetricsStoreEntry> metricsFormatter(final MetricsFormatter baseFormatter) {
        return baseFormatter
                .then(iterationToken("{metrics-names}", METRICS_NAME_FORMAT, DELIMITER,
                        baseFormatter.metricNameFormatter(), baseFormatter::metricValues, baseFormatter))
                .then(iterationToken("{metrics-values}", METRICS_VALUE_FORMAT, DELIMITER,
                        baseFormatter.metricValueFormatter(), baseFormatter::metricValues, baseFormatter));
    }

    protected ValueFormatter<MetricsStoreEntry> histogramFormatter(final HistogramFormatter baseFormatter) {
        return metricsFormatter(baseFormatter)
                .then(iterationToken("{bucket-names}", HISTOGRAM_BUCKET_NAME, DELIMITER, baseFormatter.bucketValueFormatter(),
                        (line, entryId, metric) -> {
                            final HistogramValues histogram = baseFormatter.histogramValues(line, entryId, metric).iterator().next();
                            return baseFormatter.bucketValues(line, entryId, histogram);
                        }, baseFormatter))
                .then(iterationToken("{histogram-values}", HISTOGRAM_VALUE_FORMAT, "",
                        baseFormatter.histogramValuesFormatter(HISTOGRAM_BUCKET_VALUE, DELIMITER),
                        baseFormatter::histogramValues, baseFormatter));

    }

    @Override
    public MessagePrinter<DataFrame> command() {
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(HEADER_LINE + COMMAND_FORMAT, dataFrameFormatter),
                parameterized(COMMAND_FORMAT, dataFrameFormatter)
        );
    }

    @Override
    public MessagePrinter<DataFrame> event() {
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(HEADER_LINE + EVENT_FORMAT, dataFrameFormatter),
                parameterized(EVENT_FORMAT, dataFrameFormatter)
        );
    }

    @Override
    public MessagePrinter<MetricsStoreEntry> frequencyMetrics() {
        final ValueFormatter<MetricsStoreEntry> formatter = metricsFormatter(baseMetricsFormatter);
        return composite(
                (line, entryId, entry) -> line == 0 ? 0 : 1,
                parameterized(METRICS_HEADER + METRICS_FREQUENCY_FORMAT, formatter),
                parameterized(METRICS_FREQUENCY_FORMAT, formatter)
        );
    }

    protected MessagePrinter<MetricsStoreEntry> timeMetrics(final ValueFormatter<MetricsStoreEntry> formatter) {
        return composite((line, entryId, entry) -> {
                    final Target target = entry.target();
                    switch (target) {
                        case COMMAND:
                            return line == 0 ? 0 : 1;
                        case EVENT:
                            return line == 0 ? 2 : 3;
                        case OUTPUT:
                            return line == 0 ? 4 : 5;
                        default:
                            throw new IllegalArgumentException("Invalid target: " + target);
                    }
                },
                parameterized(METRICS_HEADER + METRICS_COMMAND_FORMAT, formatter),
                parameterized(METRICS_COMMAND_FORMAT, formatter),
                parameterized(METRICS_HEADER + METRICS_EVENT_FORMAT, formatter),
                parameterized(METRICS_EVENT_FORMAT, formatter),
                parameterized(METRICS_HEADER + METRICS_OUTPUT_FORMAT, formatter),
                parameterized(METRICS_OUTPUT_FORMAT, formatter)
        );
    }

    @Override
    public MessagePrinter<MetricsStoreEntry> timeMetrics() {
        return timeMetrics(metricsFormatter(baseMetricsFormatter));
    }

    @Override
    public MessagePrinter<MetricsStoreEntry> latencyMetrics() {
        return timeMetrics(metricsFormatter(baseLatencyFormatter));
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
                parameterized(HISTOGRAM_HEADER, formatter),
                parameterized(HISTOGRAM_HEADER + HISTOGRAM_FORMAT, formatter),
                NOOP,
                parameterized(HISTOGRAM_FORMAT, formatter)
        );
    }

}
