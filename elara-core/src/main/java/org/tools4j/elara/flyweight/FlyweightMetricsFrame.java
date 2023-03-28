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
import org.tools4j.elara.plugin.metrics.FrequencyMetric;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricType;
import org.tools4j.elara.plugin.metrics.TimeMetric;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

/**
 * A flyweight frame for reading and either a {@link TimeMetricsFrame} or a {@link FrequencyMetricsFrame}.
 */
public class FlyweightMetricsFrame implements TimeMetricsFrame, FrequencyMetricsFrame, Flyweight<FlyweightMetricsFrame> {

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

    public boolean valid() {
         return timeMetrics.valid() || frequencyMetrics.valid();
    }

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
    public Target target() {
        return timeMetrics.valid() ? timeMetrics.target() : null;
    }

    @Override
    public int sourceId() {
        return timeMetrics.valid() ? timeMetrics.sourceId() : 0;
    }

    @Override
    public long sourceSequence() {
        return timeMetrics.valid() ? timeMetrics.sourceSequence() : 0;
    }

    @Override
    public int eventIndex() {
        return timeMetrics.valid() ? timeMetrics.eventIndex() : 0;
    }

    @Override
    public long eventSequence() {
        return timeMetrics.valid() ? timeMetrics.eventSequence() : 0;
    }

    @Override
    public long iteration() {
        return frequencyMetrics.valid() ? frequencyMetrics.iteration() : 0;
    }

    @Override
    public long interval() {
        return frequencyMetrics.valid() ? frequencyMetrics.interval() : 0;
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
        return timeMetrics.valid() ? timeMetric(valueIndex) : frequencyMetrics.valid() ? frequencyMetric(valueIndex) : null;
    }

    @Override
    public TimeMetric timeMetric(final int valueIndex) {
        return timeMetrics.valid() ? timeMetrics.timeMetric(valueIndex) : null;
    }

    @Override
    public FrequencyMetric frequencyMetric(final int valueIndex) {
        return frequencyMetrics.valid() ? frequencyMetrics.frequencyMetric(valueIndex) : null;
    }

    @Override
    public long timeValue(final int valueIndex) {
        return timeMetrics.valid() ? timeMetrics.timeValue(valueIndex) : 0;
    }

    @Override
    public long frequencyValue(final int valueIndex) {
        return frequencyMetrics.valid() ? frequencyMetrics.frequencyValue(valueIndex) : 0;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        return timeMetrics.valid() ? timeMetrics.writeTo(dst, dstOffset) :
                frequencyMetrics.valid() ? frequencyMetrics.writeTo(dst, dstOffset) : null ;
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        if (timeMetrics.valid()) {
            return timeMetrics.printTo(dst);
        }
        if (frequencyMetrics.valid()) {
            return frequencyMetrics.printTo(dst);
        }
        return dst.append("FlyweightMetrics{???}");
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }
}