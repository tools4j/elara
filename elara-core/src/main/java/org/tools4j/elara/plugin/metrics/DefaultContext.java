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

import org.tools4j.elara.log.MessageLog;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_SENDING_TIME;

public class DefaultContext implements Context {

    private final Set<TimeMetric> timeMetrics = EnumSet.noneOf(TimeMetric.class);
    private final Set<FrequencyMetric> frequencyMetrics = EnumSet.noneOf(FrequencyMetric.class);
    private InputSendingTimeExtractor inputSendingTimeExtractor;
    private long frequencyLogInterval;
    private MessageLog timeMetricsLog;
    private MessageLog frequencyMetricsLog;

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
    public long frequencyLogInterval() {
        return frequencyLogInterval;
    }

    @Override
    public MessageLog timeMetricsLog() {
        return timeMetricsLog;
    }

    @Override
    public MessageLog frequencyMetricsLog() {
        return frequencyMetricsLog;
    }

    @Override
    public Context timeMetric(final TimeMetric metric) {
        timeMetrics.add(metric);
        return this;
    }

    @Override
    public Context timeMetrics(final TimeMetric... metrics) {
        for (final TimeMetric metric : metrics) {
            timeMetrics.add(metric);
        }
        return this;
    }

    @Override
    public Context timeMetrics(final Set<? extends TimeMetric> metrics) {
        timeMetrics.addAll(metrics);
        return this;
    }

    @Override
    public Context frequencyMetric(final FrequencyMetric metric) {
        frequencyMetrics.add(metric);
        return this;
    }

    @Override
    public Context frequencyMetrics(final FrequencyMetric... metrics) {
        for (final FrequencyMetric metric : metrics) {
            frequencyMetrics.add(metric);
        }
        return this;
    }

    @Override
    public Context frequencyMetrics(final Set<? extends FrequencyMetric> metrics) {
        frequencyMetrics.addAll(metrics);
        return this;
    }

    @Override
    public Context latencyMetric(final LatencyMetric metric) {
        return timeMetric(metric.start()).timeMetric(metric.end());
    }

    @Override
    public Context latencyMetrics(final LatencyMetric... metrics) {
        for (final LatencyMetric metric : metrics) {
            latencyMetric(metric);
        }
        return this;
    }

    @Override
    public Context latencyMetrics(final Set<? extends LatencyMetric> metrics) {
        metrics.forEach(this::latencyMetric);
        return this;
    }

    @Override
    public Context inputSendingTimeExtractor(final InputSendingTimeExtractor sendingTimeExtractor) {
        this.inputSendingTimeExtractor = requireNonNull(sendingTimeExtractor);
        return this;
    }

    @Override
    public Context frequencyLogInterval(final long timeInterval) {
        if (timeInterval <= 0) {
            throw new IllegalArgumentException("time interval must be positive: " + timeInterval);
        }
        this.frequencyLogInterval = timeInterval;
        return this;
    }

    @Override
    public Context metricsLog(final MessageLog metricLog) {
        requireNonNull(metricLog);
        this.timeMetricsLog = metricLog;
        this.frequencyMetricsLog = metricLog;
        return this;
    }

    @Override
    public Context timeMetricsLog(final MessageLog metricLog) {
        this.timeMetricsLog = requireNonNull(metricLog);
        return this;
    }

    @Override
    public Context frequencyMetricsLog(final MessageLog metricLog) {
        this.frequencyMetricsLog = requireNonNull(metricLog);
        return this;
    }

    static Configuration validate(final Configuration configuration) {
        if (configuration.timeMetrics().isEmpty() && configuration.frequencyMetrics().isEmpty()) {
            throw new IllegalArgumentException("No time or frequency metrics are specified in the metrics plugin configuration");
        }
        if (configuration.timeMetrics().contains(INPUT_SENDING_TIME) && null == configuration.inputSendingTimeExtractor()) {
            throw new IllegalArgumentException("Metrics configuration specifies to capture " + INPUT_SENDING_TIME +
                    " but no " + InputSendingTimeExtractor.class.getSimpleName() + " is configured");
        }
        if (!configuration.frequencyMetrics().isEmpty() && 0 == configuration.frequencyLogInterval()) {
            throw new IllegalArgumentException("Metrics configuration specifies at least one frequency metric but no frequency log interval is configured");
        }
        if (!configuration.timeMetrics().isEmpty() && null == configuration.timeMetricsLog()) {
            throw new IllegalArgumentException("Metrics configuration specifies at least one time metric but no time metric log is configured");
        }
        if (!configuration.frequencyMetrics().isEmpty() && null == configuration.frequencyMetricsLog()) {
            throw new IllegalArgumentException("Metrics configuration specifies at least one frequency metric but no frequency metric log is configured");
        }
        return configuration;
    }
}
