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
package org.tools4j.elara.plugin.metrics;

import java.util.Arrays;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class DefaultMetricsState implements MetricsState {

    private final Set<TimeMetric> timeMetrics;
    private final Set<FrequencyMetric> frequencyMetrics;
    private final long[] times = new long[TimeMetric.count()];
    private final long[] counts = new long[FrequencyMetric.count()];

    public DefaultMetricsState(final Set<TimeMetric> timeMetrics, final Set<FrequencyMetric> frequencyMetrics) {
        this.timeMetrics = requireNonNull(timeMetrics);
        this.frequencyMetrics = requireNonNull(frequencyMetrics);
    }

    @Override
    public boolean capture(final TimeMetric metric) {
        return timeMetrics.contains(metric);
    }

    @Override
    public boolean capture(final FrequencyMetric metric) {
        return frequencyMetrics.contains(metric);
    }

    @Override
    public long time(final TimeMetric metric) {
        return times[metric.ordinal()];
    }

    @Override
    public long count(final FrequencyMetric metric) {
        return counts[metric.ordinal()];
    }

    @Override
    public MetricsState time(final TimeMetric metric, final long time) {
        times[metric.ordinal()] = time;
        return this;
    }

    @Override
    public MetricsState count(final FrequencyMetric metric, final long add) {
        counts[metric.ordinal()] += add;
        return this;
    }

    @Override
    public MetricsState clear(final TimeMetric metric) {
        times[metric.ordinal()] = 0;
        return this;
    }

    @Override
    public MetricsState clear(final FrequencyMetric metric) {
        counts[metric.ordinal()] = 0;
        return this;
    }

    @Override
    public MetricsState clear() {
        Arrays.fill(times, 0);
        Arrays.fill(counts, 0);
        return this;
    }
}
