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
package org.tools4j.elara.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricType;

/**
 * A flyweight frame for reading and either a {@link TimeMetricsFrame} or a {@link FrequencyMetricsFrame}.
 */
public class FlyweightMetricsFrame implements MetricsFrame, Flyweight<FlyweightMetricsFrame> {

    private final FlyweightHeader header = new FlyweightHeader(FrameDescriptor.HEADER_LENGTH);
    private final FlyweightTimeMetrics timeMetrics = new FlyweightTimeMetrics();
    private final FlyweightFrequencyMetrics frequencyMetrics = new FlyweightFrequencyMetrics();

    @Override
    public FlyweightMetricsFrame wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        final byte type = header.type();
        FrameType.validateMetricsType(type);
        if (type == FrameType.TIME_METRICS_TYPE) {
            timeMetrics.wrap(buffer, offset);
            frequencyMetrics.reset();
        } else {
            frequencyMetrics.wrap(buffer, offset);
            timeMetrics.reset();
        }
        return this;
    }

    @Override
    public boolean valid() {
         return timeMetrics.valid() || frequencyMetrics.valid();
    }

    @Override
    public FlyweightMetricsFrame reset() {
        header.reset();
        timeMetrics.reset();
        frequencyMetrics.reset();
        return this;
    }

    @Override
    public Header header() {
        return timeMetrics.valid() ? timeMetrics.header() : frequencyMetrics.valid() ? frequencyMetrics.header() : header;
    }

    @Override
    public int headerLength() {
        return timeMetrics.valid() ? timeMetrics.headerLength() : frequencyMetrics.valid() ? frequencyMetrics.headerLength() : header.headerLength();
    }

    @Override
    public MetricType metricType() {
        return timeMetrics.valid() ? timeMetrics.metricType() : frequencyMetrics.valid() ? frequencyMetrics.metricType() : null;
    }

    @Override
    public long metricTime() {
        return timeMetrics.valid() ? timeMetrics.metricTime() : frequencyMetrics.valid() ? frequencyMetrics.metricTime() : 0;
    }

    @Override
    public int valueCount() {
        return timeMetrics.valid() ? timeMetrics.valueCount() : frequencyMetrics.valid() ? frequencyMetrics.valueCount() : 0;
    }

    @Override
    public boolean hasMetric(final Metric metric) {
        return timeMetrics.valid() && timeMetrics.hasMetric(metric) || frequencyMetrics.valid() && frequencyMetrics.hasMetric(metric);
    }

    @Override
    public Metric metric(final int valueIndex) {
        return timeMetrics.valid() ? timeMetrics.timeMetric(valueIndex) : frequencyMetrics.valid() ? frequencyMetrics.frequencyMetric(valueIndex) : null;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        return timeMetrics.valid() ? timeMetrics.writeTo(dst, dstOffset) :
                frequencyMetrics.valid() ? frequencyMetrics.writeTo(dst, dstOffset) : null ;
    }

    @Override
    public void accept(final FrameVisitor visitor) {
        if (timeMetrics.valid()) {
            timeMetrics.accept(visitor);
        } else if (frequencyMetrics.valid()) {
            frequencyMetrics.accept(visitor);
        }
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        if (timeMetrics.valid()) {
            return timeMetrics.printTo(dst);
        }
        if (frequencyMetrics.valid()) {
            return frequencyMetrics.printTo(dst);
        }
        return dst.append("FlyweightMetricsFrame:???");
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }
}
