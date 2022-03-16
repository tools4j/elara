/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.format.MessagePrinter.composite;

public interface MessagePrinters {

    static MessagePrinters defaults() {
        return new DefaultMessagePrinters();
    }

    static MessagePrinters defaults(final TimeFormatter timeFormatter) {
        return defaults().withTimeFormatter(timeFormatter);
    }

    default DataFrameFormatter dataFrameFormatter() {
        return DataFrameFormatter.DEFAULT;
    }

    default MetricsFormatter metricsFormatter() {
        return MetricsFormatter.DEFAULT;
    }

    default LatencyFormatter latencyFormatter() {
        return LatencyFormatter.DEFAULT;
    }

    default HistogramFormatter histogramFormatter() {
        return HistogramFormatter.DEFAULT;
    }

    default MessagePrinters withTimeFormatter(final TimeFormatter timeFormatter) {
        requireNonNull(timeFormatter);
        return new MessagePrinters() {
            final DataFrameFormatter DATA_FRAME_FORMATTER = DataFrameFormatter.create(timeFormatter);
            final MetricsFormatter METRICS_FORMATTER = MetricsFormatter.create(timeFormatter);
            final LatencyFormatter LATENCY_FORMATTER = LatencyFormatter.create(timeFormatter);
            final HistogramFormatter HISTOGRAM_FORMATTER = HistogramFormatter.create(timeFormatter);

            @Override
            public DataFrameFormatter dataFrameFormatter() {
                return DATA_FRAME_FORMATTER;
            }

            @Override
            public MetricsFormatter metricsFormatter() {
                return METRICS_FORMATTER;
            }

            @Override
            public LatencyFormatter latencyFormatter() {
                return LATENCY_FORMATTER;
            }

            @Override
            public HistogramFormatter histogramFormatter() {
                return HISTOGRAM_FORMATTER;
            }

            @Override
            public MessagePrinter<DataFrame> command() {
                return MessagePrinters.this.command();
            }

            @Override
            public MessagePrinter<DataFrame> event() {
                return MessagePrinters.this.event();
            }

            @Override
            public MessagePrinter<MetricsLogEntry> timeMetrics() {
                return MessagePrinters.this.timeMetrics();
            }

            @Override
            public MessagePrinter<MetricsLogEntry> frequencyMetrics() {
                return MessagePrinters.this.frequencyMetrics();
            }

            @Override
            public MessagePrinter<MetricsLogEntry> latencyMetrics() {
                return MessagePrinters.this.latencyMetrics();
            }

            @Override
            public MessagePrinter<MetricsLogEntry> latencyHistogram() {
                return MessagePrinters.this.latencyMetrics();
            }
        };
    }

    default MessagePrinter<DataFrame> frame() {
        return composite(
                (line, entryId, frame) -> frame.header().index() == FlyweightCommand.INDEX ? 0 : 1,
                command(),
                event()
        );
    }

    default MessagePrinter<MetricsLogEntry> metrics() {
        return metrics(timeMetrics(), frequencyMetrics());
    }

    default MessagePrinter<MetricsLogEntry> metricsWithLatencies() {
        return metrics(latencyMetrics(), frequencyMetrics());
    }

    default MessagePrinter<MetricsLogEntry> metricsWithLatencyHistogram() {
        return metrics(latencyHistogram(), frequencyMetrics());
    }

    default MessagePrinter<MetricsLogEntry> metrics(final MessagePrinter<MetricsLogEntry> timeMetricsPrinter,
                                                    final MessagePrinter<MetricsLogEntry> frequencyMetricsPrinter) {
        return composite(
                (line, entryId, emtry) -> emtry.type() == Type.TIME ? 0 : 1,
                timeMetricsPrinter,
                frequencyMetricsPrinter
        );
    }

    MessagePrinter<DataFrame> command();
    MessagePrinter<DataFrame> event();
    MessagePrinter<MetricsLogEntry> frequencyMetrics();
    MessagePrinter<MetricsLogEntry> timeMetrics();
    MessagePrinter<MetricsLogEntry> latencyMetrics();
    MessagePrinter<MetricsLogEntry> latencyHistogram();

}
