/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import java.util.Set;

public interface Context extends Configuration {
    Context timeMetric(TimeMetric metric);
    Context timeMetrics(TimeMetric... metrics);
    Context timeMetrics(Set<? extends TimeMetric> metrics);
    Context frequencyMetric(FrequencyMetric metric);
    Context frequencyMetrics(FrequencyMetric... metrics);
    Context frequencyMetrics(Set<? extends FrequencyMetric> metrics);
    Context latencyMetric(LatencyMetric metric);
    Context latencyMetrics(LatencyMetric... metrics);
    Context latencyMetrics(Set<? extends LatencyMetric> metrics);
    Context inputSendingTimeExtractor(InputSendingTimeExtractor sendingTimeExtractor);
    Context frequencyLogInterval(long timeInterval);
    Context metricsLog(MessageLog metricLog);
    Context timeMetricsLog(MessageLog metricLog);
    Context frequencyMetricsLog(MessageLog metricLog);

    static Context create() {
        return new DefaultContext();
    }
}
