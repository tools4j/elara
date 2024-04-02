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
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.plugin.metrics.FrequencyMetric;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricType;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.FrequencyMetricsDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrequencyMetricsDescriptor.INTERVAL_OFFSET;
import static org.tools4j.elara.flyweight.FrequencyMetricsDescriptor.ITERATION_OFFSET;
import static org.tools4j.elara.flyweight.FrequencyMetricsDescriptor.METRIC_TIME_OFFSET;
import static org.tools4j.elara.flyweight.FrequencyMetricsDescriptor.METRIC_TYPES_OFFSET;
import static org.tools4j.elara.flyweight.FrequencyMetricsDescriptor.PAYLOAD_OFFSET;

/**
 * A flyweight frame for reading and writing frequency metrics data laid out as per {@link FrequencyMetricsDescriptor}.
 */
public class FlyweightFrequencyMetrics implements FrequencyMetricsFrame, Flyweight<FlyweightFrequencyMetrics>, Writable {

    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final MutableDirectBuffer payload = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightFrequencyMetrics wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        FrameType.validateFrequencyMetricsType(header.type());
        return wrapPayload(buffer, offset);
    }

    public FlyweightFrequencyMetrics wrapSilently(final DirectBuffer buffer, final int offset) {
        header.wrapSilently(buffer, offset);
        return wrapPayload(buffer, offset);
    }

    private FlyweightFrequencyMetrics wrapPayload(final DirectBuffer buffer, final int offset) {
        final int frameSize = header.frameSize();
        payload.wrap(buffer, offset + HEADER_LENGTH, frameSize - HEADER_LENGTH);
        return this;
    }

    @Override
    public boolean valid() {
        return header.valid() && FrameType.isFrequencyMetricsType(header.type());
    }

    @Override
    public FlyweightFrequencyMetrics reset() {
        header.reset();
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public int headerLength() {
        return HEADER_LENGTH;
    }

    @Override
    public MetricType metricType() {
        return MetricType.FREQUENCY;
    }

    @Override
    public long iteration() {
        return iteration(header.buffer());
    }

    public static long iteration(final DirectBuffer buffer) {
        return buffer.getLong(ITERATION_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long interval() {
        return interval(header.buffer());
    }

    public static long interval(final DirectBuffer buffer) {
        return buffer.getLong(INTERVAL_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long metricTime() {
        return metricTime(header.buffer());
    }

    public static long metricTime(final DirectBuffer buffer) {
        return buffer.getLong(METRIC_TIME_OFFSET, LITTLE_ENDIAN);
    }

    public short metricTypes() {
        return metricTypes(header.buffer());
    }

    public static short metricTypes(final DirectBuffer buffer) {
        return buffer.getShort(METRIC_TYPES_OFFSET, LITTLE_ENDIAN);
    }

    public static short metricTypes(final FrequencyMetric[] metrics) {
        return FrequencyMetric.choice(metrics);
    }

    @Override
    public boolean hasMetric(final Metric metric) {
        return metric instanceof FrequencyMetric && FrequencyMetric.contains(metricTypes(), (FrequencyMetric)metric);
    }

    @Override
    public int valueCount() {
        return valueCount(header.buffer());
    }

    public static int valueCount(final DirectBuffer buffer) {
        final short metricTypes = metricTypes(buffer);
        return FrequencyMetric.count(metricTypes);
    }

    @Override
    public FrequencyMetric frequencyMetric(final int valueIndex) {
        return frequencyMetric(header.buffer(), valueIndex);
    }

    public static FrequencyMetric frequencyMetric(final DirectBuffer buffer, final int valueIndex) {
        final short metricTypes = metricTypes(buffer);
        return FrequencyMetric.metric(metricTypes, valueIndex);
    }

    @Override
    public long frequencyValue(final int valueIndex) {
        return payload.getLong(valueIndex * Long.BYTES);
    }

    public static long frequencyValue(final DirectBuffer buffer, final int valueIndex) {
        return buffer.getLong(PAYLOAD_OFFSET + valueIndex * Long.BYTES);
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        final int payloadSize = header().frameSize() - HEADER_LENGTH;
        writeHeader(iteration(), interval(), metricTypes(), metricTime(), valueCount(), dst, dstOffset);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, 0, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    public static int writeHeader(final long iteration,
                                  final long interval,
                                  final short metricTypes,
                                  final long metricTime,
                                  final int valueCount,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        final int frameSize = HEADER_LENGTH + valueCount * Long.BYTES;
        FlyweightHeader.write(FrameType.FREQUENCY_METRICS_TYPE, metricTypes, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + ITERATION_OFFSET, iteration, LITTLE_ENDIAN);
        dst.putLong(dstOffset + INTERVAL_OFFSET, interval, LITTLE_ENDIAN);
        dst.putLong(dstOffset + METRIC_TIME_OFFSET, metricTime, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static int writeFrequencyValue(final int valueIndex,
                                          final long frequencyValue,
                                          final MutableDirectBuffer dst,
                                          final int dstOffset) {
        dst.putLong(dstOffset + PAYLOAD_OFFSET + valueIndex * Long.BYTES, frequencyValue, LITTLE_ENDIAN);
        return Long.BYTES;
    }

    @Override
    public void accept(final FrameVisitor visitor) {
        visitor.frequencyMetricsFrame(this);
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightFrequencyMetrics");
        if (valid()) {
            final Header header = header();
            dst.append(":version=").append(header.version());
            dst.append("|frame-size=").append(frameSize());
            dst.append("|metric-type=").append(metricType());
            dst.append("|iteration=").append(iteration());
            dst.append("|interval=").append(interval());
            dst.append("|metric-time=").append(metricTime());
            dst.append("|value-count=").append(valueCount());
        } else {
            dst.append(":???");
        }
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }
}
