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

import org.tools4j.elara.format.HistogramFormatter.HistogramValues;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

final class DefaultHistogramValues implements HistogramValues {
    private final MetricsLogEntry entry;
    private final Metric metric;
    private final TimeFormatter timeFormatter;

    private long[] values;
    private int count;
    private boolean sorted = true;

    DefaultHistogramValues(final MetricsLogEntry entry, final Metric metric, final TimeFormatter timeFormatter) {
        this.entry = requireNonNull(entry);
        this.metric = requireNonNull(metric);
        this.timeFormatter = requireNonNull(timeFormatter);
    }

    @Override
    public MetricsLogEntry entry() {
        return entry;
    }

    @Override
    public Metric metric() {
        return metric;
    }

    @Override
    public TimeFormatter timeFormatter() {
        return timeFormatter;
    }

    @Override
    public void record(final long value) {
        if (values == null) {
            values = new long[1024];
        }
        if (count >= values.length) {
            values = Arrays.copyOf(values, length(2 * count));
        }
        values[count] = value;
        sorted = count == 0 || (sorted && values[count - 1] <= value);
        count++;
    }

    @Override
    public void reset() {
        count = 0;
        sorted = true;
    }

    @Override
    public long count() {
        return count;
    }

    @Override
    public long min() {
        if (count == 0) {
            return 0;
        }
        sort();
        return values[0];
    }

    @Override
    public long max() {
        if (count == 0) {
            return 0;
        }
        sort();
        return values[count - 1];
    }

    @Override
    public long valueAtPercentile(final double percentile) {
        // Truncate to 0..100%, and remove 1 ulp to avoid roundoff overruns into next bucket when we
        // subsequently round up to the nearest integer:
        final double requestedPercentile =
                Math.min(Math.max(Math.nextAfter(percentile, Double.NEGATIVE_INFINITY), 0.0D), 1.0);
        final int countAtPercentile = (int)(Math.ceil(requestedPercentile * count)); // round up
        sort();
        return countAtPercentile == 0 ? 0 : values[countAtPercentile - 1];
    }

    private void sort() {
        if (sorted) {
            return;
        }
        Arrays.sort(values, 0, count);
        sorted = true;
    }

    private static int length(final int length) {
        return length < 0 ? Integer.MAX_VALUE : length;
    }

    @Override
    public String toString() {
        return "Histogram{metric=" + metric +
                ", min=" + min() +
                ", p50=" + valueAtPercentile(0.5) +
                ", p90=" + valueAtPercentile(0.9) +
                ", p99=" + valueAtPercentile(0.99) +
                ", p99.9=" + valueAtPercentile(0.999) +
                ", p99.99=" + valueAtPercentile(0.9999) +
                ", max=" + max() +
                "}";
    }
}
