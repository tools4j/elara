/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.plugin.metrics;

import java.util.Arrays;

public class DefaultMetricsState implements MetricsState {

    private final long[] times = new long[TimeMetric.count()];
    private final long[] counters = new long[FrequencyMetric.count()];

    @Override
    public long time(final TimeMetric metric) {
        return times[metric.ordinal()];
    }

    @Override
    public long counter(final FrequencyMetric metric) {
        return counters[metric.ordinal()];
    }

    @Override
    public MetricsState time(final TimeMetric metric, final long time) {
        times[metric.ordinal()] = time;
        return this;
    }

    @Override
    public MetricsState counter(final FrequencyMetric metric, final long add) {
        counters[metric.ordinal()] += add;
        return this;
    }

    @Override
    public MetricsState clear(final TimeMetric metric) {
        times[metric.ordinal()] = 0;
        return this;
    }

    @Override
    public MetricsState clear(final FrequencyMetric metric) {
        counters[metric.ordinal()] = 0;
        return this;
    }

    @Override
    public MetricsState clearTimeMetrics() {
        Arrays.fill(times, 0);
        return this;
    }

    @Override
    public MetricsState clearFrequencyMetrics() {
        Arrays.fill(counters, 0);
        return this;
    }

    @Override
    public MetricsState clear() {
        clearTimeMetrics();
        clearFrequencyMetrics();
        return this;
    }
}
