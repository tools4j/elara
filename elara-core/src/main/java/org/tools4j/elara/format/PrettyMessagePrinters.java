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
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.DataFrameFormatter.SOURCE_ID;
import static org.tools4j.elara.format.DataFrameFormatter.SOURCE_SEQUENCE;
import static org.tools4j.elara.format.HistogramFormatter.CaptureResult.PRINT;
import static org.tools4j.elara.format.MessagePrinter.NOOP;
import static org.tools4j.elara.format.MessagePrinter.composite;
import static org.tools4j.elara.format.MessagePrinter.iterationToken;
import static org.tools4j.elara.format.MessagePrinter.parameterized;

public class PrettyMessagePrinters implements MessagePrinters {

    public static final String ITEM_SEPARATOR = ", ";
    public static final String VERSION_LINE = "(elara message store format V{version}){nl}";
    public static final String COMMAND_FORMAT = "{time} | {line} - {frame-type}={source-id}:{source-sequence} | payload({payload-type}:{payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_0 = "{time} | {event-sequence} - {frame-type}={source-id}:{source-sequence}.{event-index} | payload({payload-type}:{payload-size})={payload}{nl}";
    public static final String EVENT_FORMAT_N = "{time} | {event-sequence} - {frame-type}={source-id}.{source-sequence}.{event-index} | payload({payload-type}:{payload-size})={payload}{nl}";

    public static final String METRICS_COMMAND_FORMAT = "{metric-time} | {line} - {metric-type} {frame-type}={source-id}:{source-sequence}   | {metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT_0 = "{metric-time} | {line} - {metric-type} {frame-type}={source-id}:{source-sequence}.{event-index} | {metrics-values}{nl}";
    public static final String METRICS_EVENT_FORMAT_N = "{metric-time} | {line} - {metric-type} {frame-type}={source-id}.{source-sequence}.{event-index} | {metrics-values}{nl}";
    public static final String METRICS_OUTPUT_FORMAT_0 = METRICS_EVENT_FORMAT_0;
    public static final String METRICS_OUTPUT_FORMAT_N = METRICS_EVENT_FORMAT_N;
    public static final String METRICS_FREQUENCY_FORMAT = "{metric-time} | {line} - {metric-type} indx={iteration}, intvl={interval}{time-unit} | {metrics-values}{nl}";
    public static final String METRICS_NAME_FORMAT = "{sep}{metric-name}";
    public static final String METRICS_VALUE_FORMAT = "{sep}{metric-name}={metric-value}";
    public static final String LATENCY_VALUE_FORMAT = "{sep}{metric-name}={metric-value}{time-unit}";
    public static final String HISTOGRAM_FORMAT = "{histogram-values}";
    public static final String HISTOGRAM_VALUE_FORMAT = "{metric-time} | {line} - {metric-type} indx={iteration}, intvl={interval}{time-unit} | {metric-name} - n={value-count}, {bucket-values}{nl}";
    public static final String HISTOGRAM_BUCKET_VALUE = "{sep}{bucket-name}={bucket-value}{time-unit}";
    private final CommandFormatter commandFormatter;
    private final EventFormatter eventFormatter;
    private final TimeMetricsFormatter timeMetricsFormatter;
    private final FrequencyMetricsFormatter frequencyMetricsFormatter;
    private final LatencyFormatter latencyFormatter;
    private final HistogramFormatter histogramFormatter;

    public PrettyMessagePrinters() {
        this(CommandFormatter.DEFAULT, EventFormatter.DEFAULT, TimeMetricsFormatter.DEFAULT,
                FrequencyMetricsFormatter.DEFAULT, LatencyFormatter.DEFAULT, HistogramFormatter.DEFAULT);
    }

    public PrettyMessagePrinters(final TimeFormatter timeFormatter, final long interval) {
        this(CommandFormatter.create(timeFormatter), EventFormatter.create(timeFormatter),
                TimeMetricsFormatter.create(timeFormatter), FrequencyMetricsFormatter.create(timeFormatter),
                LatencyFormatter.create(timeFormatter), HistogramFormatter.create(timeFormatter, interval));
    }

    public PrettyMessagePrinters(final CommandFormatter commandFormatter,
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

    @Override
    public MessagePrinter<CommandFrame> command() {
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(VERSION_LINE + COMMAND_FORMAT, commandFormatter),
                parameterized(COMMAND_FORMAT, commandFormatter)
        );
    }

    @Override
    public MessagePrinter<EventFrame> event() {
        final ValueFormatter<EventFrame> formatter0 = eventFormatter;
        final ValueFormatter<EventFrame> formatterN = Spacer.spacer(formatter0, '.', SOURCE_ID, SOURCE_SEQUENCE);
        return composite((line, entryId, frame) -> {
                    if (line == 0) return 0;
                    if (frame.eventIndex() == 0) return 1;
                    return 2;
                },
                parameterized(VERSION_LINE + EVENT_FORMAT_0, formatter0),
                parameterized(EVENT_FORMAT_0, formatter0),
                parameterized(EVENT_FORMAT_N, formatterN)
        );
    }

    protected <F extends MetricsFrame> ValueFormatter<F> metricsFormatter(final MetricsFormatter<F> baseFormatter,
                                                                          final String metricsValueFormat) {
        return baseFormatter
                .then(iterationToken("{metrics-names}", METRICS_NAME_FORMAT, ITEM_SEPARATOR,
                        baseFormatter.metricNameFormatter(), baseFormatter::metricValues, baseFormatter))
                .then(iterationToken("{metrics-values}", metricsValueFormat, ITEM_SEPARATOR,
                        baseFormatter.metricValueFormatter(), baseFormatter::metricValues, baseFormatter));
    }

    protected ValueFormatter<TimeMetricsFrame> histogramFormatter(final HistogramFormatter baseFormatter) {
        return metricsFormatter(baseFormatter, LATENCY_VALUE_FORMAT)
                .then(iterationToken("{metrics-names}", METRICS_NAME_FORMAT, ITEM_SEPARATOR,
                        baseFormatter.metricNameFormatter(), baseFormatter::metricValues, baseFormatter))
                .then(iterationToken("{histogram-values}", HISTOGRAM_VALUE_FORMAT, "",
                        baseFormatter.histogramValuesFormatter(HISTOGRAM_BUCKET_VALUE, ITEM_SEPARATOR),
                        baseFormatter::histogramValues, baseFormatter));

    }

    protected MessagePrinter<TimeMetricsFrame> timeMetrics(final ValueFormatter<TimeMetricsFrame> formatter) {
        final ValueFormatter<TimeMetricsFrame> formatter0 = requireNonNull(formatter);
        final ValueFormatter<TimeMetricsFrame> formatterN = Spacer.spacer(formatter0, '.', SOURCE_ID, SOURCE_SEQUENCE);
        return composite((line, entryId, frame) -> {
                    final Target target = frame.target();
                    if (target != null) {
                        switch (target) {
                            case COMMAND:
                                return line == 0 ? 0 : 1;
                            case EVENT:
                                return line == 0 ? 2 : frame.eventIndex() == 0 ? 3 : 4;
                            case OUTPUT:
                                return line == 0 ? 5 : frame.eventIndex() == 0 ? 6 : 7;
                        }
                    }
                    throw new IllegalArgumentException("Invalid target: " + target);
                },
                parameterized(VERSION_LINE + METRICS_COMMAND_FORMAT, formatter0),
                parameterized(METRICS_COMMAND_FORMAT, formatter0),
                parameterized(VERSION_LINE + METRICS_EVENT_FORMAT_0, formatter0),
                parameterized(METRICS_EVENT_FORMAT_0, formatter0),
                parameterized(METRICS_EVENT_FORMAT_N, formatterN),
                parameterized(VERSION_LINE + METRICS_OUTPUT_FORMAT_0, formatter0),
                parameterized(METRICS_OUTPUT_FORMAT_0, formatter0),
                parameterized(METRICS_OUTPUT_FORMAT_N, formatterN)
        );
    }

    @Override
    public MessagePrinter<FrequencyMetricsFrame> frequencyMetrics() {
        final ValueFormatter<FrequencyMetricsFrame> formatter = metricsFormatter(frequencyMetricsFormatter, METRICS_VALUE_FORMAT);
        return composite(
                (line, entryId, frame) -> line == 0 ? 0 : 1,
                parameterized(VERSION_LINE + METRICS_FREQUENCY_FORMAT, formatter),
                parameterized(METRICS_FREQUENCY_FORMAT, formatter)
        );
    }

    @Override
    public MessagePrinter<TimeMetricsFrame> timeMetrics() {
        return timeMetrics(metricsFormatter(timeMetricsFormatter, METRICS_VALUE_FORMAT));
    }

    @Override
    public MessagePrinter<TimeMetricsFrame> latencyMetrics() {
        return timeMetrics(metricsFormatter(latencyFormatter, LATENCY_VALUE_FORMAT));
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
                parameterized(VERSION_LINE, formatter),
                parameterized(VERSION_LINE + HISTOGRAM_FORMAT, formatter),
                NOOP,
                parameterized(HISTOGRAM_FORMAT, formatter)
        );
    }

}
