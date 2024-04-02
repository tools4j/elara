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

import org.tools4j.elara.flyweight.CommandFrame;
import org.tools4j.elara.flyweight.EventFrame;
import org.tools4j.elara.flyweight.FrequencyMetricsFrame;
import org.tools4j.elara.flyweight.MetricsFrame;
import org.tools4j.elara.flyweight.TimeMetricsFrame;
import org.tools4j.elara.format.HistogramFormatter.HistogramValues;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.HistogramFormatter.CaptureResult.PRINT;
import static org.tools4j.elara.format.MessagePrinter.NOOP;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.iterationToken;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public class CsvMessagePrinters implements MessagePrinters {

    public static final String DELIMITER                = ",";
    public static final String HEADER_LINE              = "Time,Seq,Type,Source-ID,Source-Seq,Event-Index,Payload-Type,Payload-Size,Payload{nl}";
    public static final String COMMAND_FORMAT           = "{time},{line},CMD,{source-id},{source-sequence},,{payload-type},{payload-size},{payload}{nl}";
    public static final String EVENT_FORMAT             = "{time},{event-sequence},EVT,{source-id},{source-sequence},{event-index},{payload-type},{payload-size},{payload}{nl}";

    public static final String METRICS_HEADER           = "Time,Seq,Type,Source-ID,Source-Seq,Index,Interval,Unit,{metrics-names}{nl}";
    public static final String METRICS_COMMAND_FORMAT   = "{metric-time},{line},{metric-type},{source-id},{source-sequence},,,,{time-unit},{metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT     = "{metric-time},{line},{metric-type},{source-id},{source-sequence},{event-index},,,{time-unit},{metrics-values}{nl}";
    public static final String METRICS_OUTPUT_FORMAT    = METRICS_EVENT_FORMAT;
    public static final String METRICS_FREQUENCY_FORMAT = "{metric-time},{line},{metric-type},,,{iteration},{interval},{time-unit},{metrics-values}{nl}";
    public static final String METRICS_NAME_FORMAT      = "{sep}{metric-name}";
    public static final String METRICS_VALUE_FORMAT     = "{sep}{metric-value}";
    public static final String HISTOGRAM_HEADER         = "Time,Seq,Type,Source-ID,Source-Seq,Index,Interval,Unit,Metric,Count,{bucket-names}{nl}";
    public static final String HISTOGRAM_FORMAT         = "{histogram-values}";
    public static final String HISTOGRAM_VALUE_FORMAT   = "{metric-time},{line},{metric-type},,,{iteration},{interval},{time-unit},{metric-name},{value-count},{bucket-values}{nl}";
    public static final String HISTOGRAM_BUCKET_NAME    = "{sep}{bucket-name}";
    public static final String HISTOGRAM_BUCKET_VALUE   = "{sep}{bucket-value}";

    private final CommandFormatter commandFormatter;
    private final EventFormatter eventFormatter;
    private final TimeMetricsFormatter timeMetricsFormatter;
    private final FrequencyMetricsFormatter frequencyMetricsFormatter;
    private final LatencyFormatter latencyFormatter;
    private final HistogramFormatter histogramFormatter;

    public CsvMessagePrinters() {
        this(CommandFormatter.DEFAULT, EventFormatter.DEFAULT, TimeMetricsFormatter.DEFAULT,
                FrequencyMetricsFormatter.DEFAULT, LatencyFormatter.DEFAULT, HistogramFormatter.DEFAULT);
    }

    public CsvMessagePrinters(final TimeFormatter timeFormatter, final long interval) {
        this(CommandFormatter.create(timeFormatter), EventFormatter.create(timeFormatter),
                TimeMetricsFormatter.create(timeFormatter), FrequencyMetricsFormatter.create(timeFormatter),
                LatencyFormatter.create(timeFormatter), HistogramFormatter.create(timeFormatter, interval));
    }

    public CsvMessagePrinters(final CommandFormatter commandFormatter,
                              final EventFormatter eventFormatter,
                              final TimeMetricsFormatter timeMetricsFormatter,
                              final FrequencyMetricsFormatter frequencyMetricsFormatter,
                              final LatencyFormatter latencyFormatter,
                              final HistogramFormatter histogramFormatter) {
        this.commandFormatter = requireNonNull(commandFormatter);
        this.eventFormatter = requireNonNull(eventFormatter);
        this.timeMetricsFormatter = requireNonNull(timeMetricsFormatter);
        this.frequencyMetricsFormatter = requireNonNull(frequencyMetricsFormatter);
        this.latencyFormatter = requireNonNull(latencyFormatter);
        this.histogramFormatter = requireNonNull(histogramFormatter);
    }

    protected <F extends MetricsFrame> ValueFormatter<F> metricsFormatter(final MetricsFormatter<F> baseFormatter) {
        return baseFormatter
                .then(iterationToken("{metrics-names}", METRICS_NAME_FORMAT, DELIMITER,
                        baseFormatter.metricNameFormatter(), baseFormatter::metricValues, baseFormatter))
                .then(iterationToken("{metrics-values}", METRICS_VALUE_FORMAT, DELIMITER,
                        baseFormatter.metricValueFormatter(), baseFormatter::metricValues, baseFormatter));
    }

    protected ValueFormatter<TimeMetricsFrame> histogramFormatter(final HistogramFormatter baseFormatter) {
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
    public MessagePrinter<CommandFrame> command() {
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(HEADER_LINE + COMMAND_FORMAT, commandFormatter),
                parameterized(COMMAND_FORMAT, commandFormatter)
        );
    }

    @Override
    public MessagePrinter<EventFrame> event() {
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(HEADER_LINE + EVENT_FORMAT, eventFormatter),
                parameterized(EVENT_FORMAT, eventFormatter)
        );
    }

    @Override
    public MessagePrinter<FrequencyMetricsFrame> frequencyMetrics() {
        final ValueFormatter<FrequencyMetricsFrame> formatter = metricsFormatter(frequencyMetricsFormatter);
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(METRICS_HEADER + METRICS_FREQUENCY_FORMAT, formatter),
                parameterized(METRICS_FREQUENCY_FORMAT, formatter)
        );
    }

    protected MessagePrinter<TimeMetricsFrame> timeMetrics(final ValueFormatter<TimeMetricsFrame> formatter) {
        return composite((line, entryId, frame) -> {
                    final Target target = frame.target();
                    if (target != null) {
                        switch (target) {
                            case COMMAND:
                                return line == 0 ? 0 : 1;
                            case EVENT:
                                return line == 0 ? 2 : 3;
                            case OUTPUT:
                                return line == 0 ? 4 : 5;
                        }
                    }
                    throw new IllegalArgumentException("Invalid target: " + target);
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
    public MessagePrinter<TimeMetricsFrame> timeMetrics() {
        return timeMetrics(metricsFormatter(timeMetricsFormatter));
    }

    @Override
    public MessagePrinter<TimeMetricsFrame> latencyMetrics() {
        return timeMetrics(metricsFormatter(latencyFormatter));
    }

    @Override
    public MessagePrinter<TimeMetricsFrame> latencyHistogram() {
        final ValueFormatter<TimeMetricsFrame> formatter = histogramFormatter(histogramFormatter);
        return composite(
                (line, entryId, frame) -> {
                    if (line == 0) {
                        histogramFormatter.latencyHistograms.remove();
                    }
                    final boolean print = histogramFormatter.capture(line, entryId, frame) == PRINT;
                    return line == 0 ? (print ? 1 : 0) : (print ? 3 : 2);
                },
                parameterized(HISTOGRAM_HEADER, formatter),
                parameterized(HISTOGRAM_HEADER + HISTOGRAM_FORMAT, formatter),
                NOOP,
                parameterized(HISTOGRAM_FORMAT, formatter)
        );
    }

}
