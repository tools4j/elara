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
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricType;
import org.tools4j.elara.plugin.metrics.TimeMetric;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import java.util.Set;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.EVENT_INDEX_OFFSET;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.EVENT_SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.METRIC_TIME_OFFSET;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.METRIC_TYPES_OFFSET;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.SOURCE_ID_OFFSET;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.SOURCE_SEQUENCE_OFFSET;

/**
 * A flyweight frame for reading and writing time metrics data laid out as per {@link TimeMetricsDescriptor}.
 */
public class FlyweightTimeMetrics implements TimeMetricsFrame, Flyweight<FlyweightTimeMetrics>, Writable {

    private static final int EVENT_FLAG = 0x80000000;
    private static final int OUTPUT_FLAG = 0x40000000;
    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final MutableDirectBuffer payload = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightTimeMetrics wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        FrameType.validateTimeMetricsType(header.type());
        return wrapPayload(buffer, offset);
    }

    public FlyweightTimeMetrics wrapSilently(final DirectBuffer buffer, final int offset) {
        header.wrapSilently(buffer, offset);
        return wrapPayload(buffer, offset);
    }

    private FlyweightTimeMetrics wrapPayload(final DirectBuffer buffer, final int offset) {
        final int frameSize = header.frameSize();
        payload.wrap(buffer, offset + HEADER_LENGTH, frameSize - HEADER_LENGTH);
        return this;
    }

    public boolean valid() {
        return header.valid();
    }

    public FlyweightTimeMetrics reset() {
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
        return MetricType.TIME;
    }

    @Override
    public int sourceId() {
        return sourceId(header.buffer());
    }

    public static int sourceId(final DirectBuffer buffer) {
        return buffer.getInt(SOURCE_ID_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long sourceSequence() {
        return sourceSequence(header.buffer());
    }

    public static long sourceSequence(final DirectBuffer buffer) {
        return buffer.getLong(SOURCE_SEQUENCE_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int eventIndex() {
        return eventIndex(header.buffer());
    }

    public static int eventIndex(final DirectBuffer buffer) {
        return 0x7fff & buffer.getShort(EVENT_INDEX_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long eventSequence() {
        return eventSequence(header.buffer());
    }

    public static long eventSequence(final DirectBuffer buffer) {
        return buffer.getLong(EVENT_SEQUENCE_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long metricTime() {
        return metricTime(header.buffer());
    }

    public static long metricTime(final DirectBuffer buffer) {
        return buffer.getLong(METRIC_TIME_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public Target target() {
        return target(header.buffer());
    }

    public static Target target(final DirectBuffer buffer) {
        return target(buffer.getInt(METRIC_TYPES_OFFSET, LITTLE_ENDIAN));
    }

    public static Target target(final int metricTypes) {
        return (metricTypes & EVENT_FLAG) == 0 ? Target.COMMAND :
                (metricTypes & OUTPUT_FLAG) == 0 ? Target.EVENT : Target.OUTPUT;
    }

    public int metricTypes() {
        return metricTypes(header.buffer());
    }

    public static int metricTypes(final DirectBuffer buffer) {
        return buffer.getInt(METRIC_TYPES_OFFSET, LITTLE_ENDIAN);
    }

    public static int metricTypes(final Target target, final Set<TimeMetric> metrics) {
        int metricTypes = 0xff & target.flags(metrics);
        if (target == Target.COMMAND) {
            return metricTypes;
        }
        metricTypes |= EVENT_FLAG;
        if (target == Target.OUTPUT) {
            metricTypes |= OUTPUT_FLAG;
        }
        return metricTypes;
    }

    public static byte metricTypesFlags(final DirectBuffer buffer) {
        return metricTypesFlags(metricTypes(buffer));
    }

    public static byte metricTypesFlags(final int metricTypes) {
        return (byte)(0xff & metricTypes);
    }

    @Override
    public int valueCount() {
        return valueCount(header.buffer());
    }

    public static int valueCount(final DirectBuffer buffer) {
        final int metricTypes = metricTypes(buffer);
        return target(metricTypes).count(metricTypesFlags(metricTypes));
    }

    @Override
    public boolean hasMetric(final Metric metric) {
        return metric instanceof TimeMetric &&
                target().contains(metricTypesFlags(metricTypes()), (TimeMetric)metric);
    }

    @Override
    public TimeMetric timeMetric(final int valueIndex) {
        return timeMetric(header.buffer(), valueIndex);
    }

    public static TimeMetric timeMetric(final DirectBuffer buffer, final int valueIndex) {
        final int metricTypes = metricTypes(buffer);
        return target(metricTypes).metric(metricTypesFlags(metricTypes), valueIndex);
    }

    @Override
    public long timeValue(final int valueIndex) {
        return payload.getLong(valueIndex * Long.BYTES);
    }

    public static long timeValue(final DirectBuffer buffer, final int valueIndex) {
        return buffer.getLong(PAYLOAD_OFFSET + valueIndex * Long.BYTES);
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        final int payloadSize = header().frameSize() - HEADER_LENGTH;
        writeHeader(sourceId(), sourceSequence(), (short)eventIndex(), eventSequence(),
                metricTypes(), metricTime(), valueCount(), dst, dstOffset);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, 0, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    public static int writeHeader(final int sourceId,
                                  final long sourceSequence,
                                  final int metricTypes,
                                  final long metricTime,
                                  final int valueCount,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        assert (metricTypes & (EVENT_FLAG | OUTPUT_FLAG)) == 0;
        return writeHeader(sourceId, sourceSequence, (short)0, 0L, metricTypes, metricTime, valueCount,
                dst, dstOffset);
    }

    public static int writeHeader(final int sourceId,
                                  final long sourceSequence,
                                  final short eventIndex,
                                  final long eventSequence,
                                  final int metricTypes,
                                  final long metricTime,
                                  final int valueCount,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        assert eventIndex >= 0;
        final int frameSize = HEADER_LENGTH + valueCount * Long.BYTES;
        FlyweightHeader.write(FrameType.TIME_METRICS_TYPE, eventIndex, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + SOURCE_ID_OFFSET,
                (0xffffffffL & sourceId) | ((0xffffffffL & metricTypes) << 32),
                LITTLE_ENDIAN);
        dst.putLong(dstOffset + EventDescriptor.SOURCE_SEQUENCE_OFFSET, sourceSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + EventDescriptor.EVENT_SEQUENCE_OFFSET, eventSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + METRIC_TIME_OFFSET, metricTime, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static int writeTimeValue(final int valueIndex,
                                     final long timeValue,
                                     final MutableDirectBuffer dst,
                                     final int dstOffset) {
        dst.putLong(dstOffset + PAYLOAD_OFFSET + valueIndex * Long.BYTES, timeValue, LITTLE_ENDIAN);
        return Long.BYTES;
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightTimeMetrics{");
        if (valid()) {
            final Header header = header();
            dst.append("version=").append(header.version());
            dst.append("|frame-size=").append(frameSize());
            dst.append("|metric-type=").append(sourceId());
            dst.append("|target=").append(target());
            dst.append("|source-id=").append(sourceId());
            dst.append("|source-seq=").append(sourceSequence());
            dst.append("|event-index=").append(eventIndex());
            dst.append("|event-seq=").append(eventSequence());
            dst.append("|metric-time=").append(metricTime());
            dst.append("|value-count=").append(valueCount());
        } else {
            dst.append("???");
        }
        dst.append('}');
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }
}
