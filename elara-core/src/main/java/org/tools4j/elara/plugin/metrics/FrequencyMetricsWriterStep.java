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
package org.tools4j.elara.plugin.metrics;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.flyweight.FlyweightFrequencyMetrics;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import java.util.Set;

import static java.util.Objects.requireNonNull;

public class FrequencyMetricsWriterStep implements AgentStep {

    private final TimeSource timeSource;
    private final long interval;
    private final MetricsState state;
    private final Appender appender;
    private final FrequencyMetric[] metrics;
    private final short metricTypes;
    private long repetition;
    private long lastWriteTime;

    public FrequencyMetricsWriterStep(final TimeSource timeSource,
                                      final MetricsConfig configuration,
                                      final MetricsState state) {
        this.timeSource = requireNonNull(timeSource);
        this.interval = configuration.frequencyMetricInterval();
        this.state = requireNonNull(state);
        this.metrics = metrics(configuration.frequencyMetrics());
        this.metricTypes = FlyweightFrequencyMetrics.metricTypes(metrics);
        if (interval <= 0) {
            throw new IllegalArgumentException("configuration.frequencyMetricInterval() must be positive: " + interval);
        }
        if (metrics.length == 0) {
            throw new IllegalArgumentException("Configuration contains no frequency metrics");
        }
        this.appender = requireNonNull(configuration.frequencyMetricsStore(), "configuration.frequencyMetricsStore()")
                .appender();
    }

    private static FrequencyMetric[] metrics(final Set<FrequencyMetric> metrics) {
        final FrequencyMetric[] array = new FrequencyMetric[metrics.size()];
        int index = 0;
        for (int ordinal = 0; ordinal < FrequencyMetric.length(); ordinal++) {
            final FrequencyMetric metric = FrequencyMetric.byOrdinal(ordinal);
            if (metrics.contains(metric)) {
                array[index++] = metric;
            }
        }
        assert index == metrics.size();
        return array;
    }

    @Override
    public int doWork() {
        int workDone = 0;
        if (repetition == 0) {
            state.clearFrequencyMetrics();
            workDone++;
        }
        final long time = timeSource.currentTime();
        if (time - lastWriteTime >= interval || repetition == 0) {
            writeMetrics(time);
            state.clearFrequencyMetrics();
            lastWriteTime = time;
            repetition++;
            workDone++;
        }
        //NOTE: - we always perform some work by checking the time
        //      - returning always true would essentially enforce busy spinning and disable any idle strategy
        //      - a reasonably configured idle strategy should never cause any serious metrics logging problems
        return workDone;
    }

    private void writeMetrics(final long time) {
        try (final AppendingContext context = appender.appending()) {
            final MutableDirectBuffer buffer = context.buffer();
            //NOTE: our repetition is intentionally a long so we can also use the sign bit before overflow
            int length = FlyweightFrequencyMetrics.writeHeader(repetition, interval, metricTypes, time, metrics.length,
                    buffer, 0);
            for (int i = 0; i < metrics.length; i++) {
                final FrequencyMetric metric = metrics[i];
                final long counter = state.counter(metric);
                length += FlyweightFrequencyMetrics.writeFrequencyValue(i, counter, buffer, 0);
            }
            context.commit(length);
        }
    }
}
