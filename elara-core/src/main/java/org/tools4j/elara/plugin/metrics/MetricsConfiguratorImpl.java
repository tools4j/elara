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
package org.tools4j.elara.plugin.metrics;

import org.tools4j.elara.store.MessageStore;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_SENDING_TIME;

public class MetricsConfiguratorImpl implements MetricsConfigurator {

    private final Set<TimeMetric> timeMetrics = EnumSet.noneOf(TimeMetric.class);
    private final Set<FrequencyMetric> frequencyMetrics = EnumSet.noneOf(FrequencyMetric.class);
    private InputSendingTimeExtractor inputSendingTimeExtractor;
    private long frequencyMetricInterval;
    private MessageStore timeMetricsStore;
    private MessageStore frequencyMetricsStore;

    @Override
    public Set<TimeMetric> timeMetrics() {
        return timeMetrics;
    }

    @Override
    public Set<FrequencyMetric> frequencyMetrics() {
        return frequencyMetrics;
    }

    @Override
    public InputSendingTimeExtractor inputSendingTimeExtractor() {
        return inputSendingTimeExtractor;
    }

    @Override
    public long frequencyMetricInterval() {
        return frequencyMetricInterval;
    }

    @Override
    public MessageStore timeMetricsStore() {
        return timeMetricsStore;
    }

    @Override
    public MessageStore frequencyMetricsStore() {
        return frequencyMetricsStore;
    }

    @Override
    public MetricsConfigurator timeMetric(final TimeMetric metric) {
        timeMetrics.add(metric);
        return this;
    }

    @Override
    public MetricsConfigurator timeMetrics(final TimeMetric... metrics) {
        for (final TimeMetric metric : metrics) {
            timeMetrics.add(metric);
        }
        return this;
    }

    @Override
    public MetricsConfigurator timeMetrics(final Set<? extends TimeMetric> metrics) {
        timeMetrics.addAll(metrics);
        return this;
    }

    @Override
    public MetricsConfigurator frequencyMetric(final FrequencyMetric metric) {
        frequencyMetrics.add(metric);
        return this;
    }

    @Override
    public MetricsConfigurator frequencyMetrics(final FrequencyMetric... metrics) {
        for (final FrequencyMetric metric : metrics) {
            frequencyMetrics.add(metric);
        }
        return this;
    }

    @Override
    public MetricsConfigurator frequencyMetrics(final Set<? extends FrequencyMetric> metrics) {
        frequencyMetrics.addAll(metrics);
        return this;
    }

    @Override
    public MetricsConfigurator latencyMetric(final LatencyMetric metric) {
        return timeMetric(metric.start()).timeMetric(metric.end());
    }

    @Override
    public MetricsConfigurator latencyMetrics(final LatencyMetric... metrics) {
        for (final LatencyMetric metric : metrics) {
            latencyMetric(metric);
        }
        return this;
    }

    @Override
    public MetricsConfigurator latencyMetrics(final Set<? extends LatencyMetric> metrics) {
        metrics.forEach(this::latencyMetric);
        return this;
    }

    @Override
    public MetricsConfigurator inputSendingTimeExtractor(final InputSendingTimeExtractor sendingTimeExtractor) {
        this.inputSendingTimeExtractor = requireNonNull(sendingTimeExtractor);
        return this;
    }

    @Override
    public MetricsConfigurator frequencyMetricInterval(final long timeInterval) {
        if (timeInterval <= 0) {
            throw new IllegalArgumentException("time interval must be positive: " + timeInterval);
        }
        this.frequencyMetricInterval = timeInterval;
        return this;
    }

    @Override
    public MetricsConfigurator metricsStore(final MessageStore metricsStore) {
        requireNonNull(metricsStore);
        this.timeMetricsStore = metricsStore;
        this.frequencyMetricsStore = metricsStore;
        return this;
    }

    @Override
    public MetricsConfigurator timeMetricsStore(final MessageStore metricStore) {
        this.timeMetricsStore = requireNonNull(metricStore);
        return this;
    }

    @Override
    public MetricsConfigurator frequencyMetricsStore(final MessageStore metricStore) {
        this.frequencyMetricsStore = requireNonNull(metricStore);
        return this;
    }

    static MetricsConfig validate(final MetricsConfig configuration) {
        if (configuration.timeMetrics().isEmpty() && configuration.frequencyMetrics().isEmpty()) {
            throw new IllegalArgumentException("No time or frequency metrics are specified in the metrics plugin configuration");
        }
        if (configuration.timeMetrics().contains(INPUT_SENDING_TIME) && null == configuration.inputSendingTimeExtractor()) {
            throw new IllegalArgumentException("Metrics configuration specifies to capture " + INPUT_SENDING_TIME +
                    " but no " + InputSendingTimeExtractor.class.getSimpleName() + " is configured");
        }
        if (!configuration.frequencyMetrics().isEmpty() && 0 == configuration.frequencyMetricInterval()) {
            throw new IllegalArgumentException("Metrics configuration specifies at least one frequency metric but no frequency store interval is configured");
        }
        if (!configuration.timeMetrics().isEmpty() && null == configuration.timeMetricsStore()) {
            throw new IllegalArgumentException("Metrics configuration specifies at least one time metric but no time metric store is configured");
        }
        if (!configuration.frequencyMetrics().isEmpty() && null == configuration.frequencyMetricsStore()) {
            throw new IllegalArgumentException("Metrics configuration specifies at least one frequency metric but no frequency metric store is configured");
        }
        return configuration;
    }
}
