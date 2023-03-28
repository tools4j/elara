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

import org.tools4j.elara.flyweight.FrequencyMetricsFrame;
import org.tools4j.elara.plugin.metrics.Metric;

import static java.util.Objects.requireNonNull;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing frequency metrics data produced by the
 * {@link org.tools4j.elara.plugin.metrics.MetricsPlugin MetricsPlugin}.
 */
public interface FrequencyMetricsFormatter extends MetricsFormatter<FrequencyMetricsFrame> {

    FrequencyMetricsFormatter DEFAULT = new FrequencyMetricsFormatter() {};

    static FrequencyMetricsFormatter create(final TimeFormatter timeFormatter) {
        requireNonNull(timeFormatter);
        return new FrequencyMetricsFormatter() {
            @Override
            public TimeFormatter timeFormatter() {
                return timeFormatter;
            }
        };
    }

    interface MetricValue {
        Metric metric();
        long value();
    }

    /** Placeholder in format string for iteration value */
    String ITERATION = "{iteration}";
    /** Placeholder in format string for interval value */
    String INTERVAL = "{interval}";

    default Object iteration(long line, long entryId, FrequencyMetricsFrame frame) {
        return frame.iteration();
    }
    default Object interval(long line, long entryId, FrequencyMetricsFrame frame) {
        return timeFormatter().formatDuration(frame.interval());
    }

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final FrequencyMetricsFrame frame) {
        switch (placeholder) {
            case ITERATION: return iteration(entryId, entryId, frame);
            case INTERVAL: return interval(entryId, entryId, frame);
            default: return MetricsFormatter.super.value(placeholder, line, entryId, frame);
        }
    }

    @Override
    default MetricsFormatter.MetricValue metricValue(long line, long entryId, FrequencyMetricsFrame frame, int valueIndex) {
        return new DefaultMetricValue(frame.metric(valueIndex), frame.frequencyValue(valueIndex));
    }
}
