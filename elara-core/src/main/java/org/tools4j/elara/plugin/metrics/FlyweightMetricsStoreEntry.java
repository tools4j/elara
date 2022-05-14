/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.flyweight.Flyweight;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import static java.lang.Integer.toBinaryString;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.CHOICE_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.INTERVAL_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.REPETITION_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.SEQUENCE_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.SOURCE_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.TIME_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.VERSION;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.VERSION_OFFSET;

public class FlyweightMetricsStoreEntry implements MetricsStoreEntry, Flyweight<FlyweightMetricsStoreEntry> {

    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightMetricsStoreEntry init(final DirectBuffer src, final int srcOffset) {
        final int length = src.capacity() - srcOffset;
        if (length < MetricsDescriptor.HEADER_LENGTH) {
            throw new IllegalArgumentException("Invalid metrics store entry, expected min length + " + MetricsDescriptor.HEADER_LENGTH + " but found only " + length);
        }
        validateVersion(src.getByte(srcOffset + MetricsDescriptor.VERSION_OFFSET));
        buffer.wrap(src, srcOffset + HEADER_OFFSET, HEADER_LENGTH);
        final int count = count();
        buffer.wrap(src, srcOffset + HEADER_OFFSET, HEADER_LENGTH + count * Long.BYTES);
        return this;
    }

    public boolean valid() {
        return buffer.capacity() >= MetricsDescriptor.HEADER_LENGTH;
    }

    public FlyweightMetricsStoreEntry reset() {
        buffer.wrap(0, 0);
        return this;
    }

    @Override
    public short version() {
        return MetricsDescriptor.version(buffer);
    }

    @Override
    public byte flags() {
        return MetricsDescriptor.flags(buffer);
    }

    @Override
    public short index() {
        return MetricsDescriptor.index(buffer);
    }

    @Override
    public int source() {
        return MetricsDescriptor.source(buffer);
    }

    @Override
    public long sequence() {
        return MetricsDescriptor.sequence(buffer);
    }

    @Override
    public long time() {
        return MetricsDescriptor.time(buffer);
    }

    @Override
    public short choice() {
        return MetricsDescriptor.choice(buffer);
    }

    @Override
    public int repetition() {
        return MetricsDescriptor.repetition(buffer);
    }

    @Override
    public long interval() {
        return MetricsDescriptor.interval(buffer);
    }

    @Override
    public Type type() {
        return MetricsDescriptor.isTimeMetrics(buffer) ? Type.TIME : Type.FREQUENCY;
    }

    @Override
    public Target target() {
        return Target.target(flags(), index());
    }

    @Override
    public int count() {
        return MetricsDescriptor.isTimeMetrics(buffer) ? target().count(flags()) : FrequencyMetric.count(choice());
    }

    @Override
    public long time(final int index) {
        return MetricsDescriptor.time(index, buffer);
    }

    @Override
    public long counter(final int index) {
        return MetricsDescriptor.counter(index, buffer);
    }

    public static int writeTimeMetricsHeader(final byte flags,
                                             final short index,
                                             final int source,
                                             final long sequence,
                                             final long time,
                                             final MutableDirectBuffer dst,
                                             final int dstOffset) {
        dst.putByte(dstOffset + VERSION_OFFSET, VERSION);
        dst.putByte(dstOffset + FLAGS_OFFSET, flags);
        dst.putShort(dstOffset + INDEX_OFFSET, index);
        dst.putInt(dstOffset + SOURCE_OFFSET, source);
        dst.putLong(dstOffset + SEQUENCE_OFFSET, sequence);
        dst.putLong(dstOffset + TIME_OFFSET, time);
        return HEADER_LENGTH;
    }

    public static int writeTime(final long time,
                                final MutableDirectBuffer dst,
                                final int dstOffset) {
        dst.putLong(dstOffset, time);
        return Long.BYTES;
    }

    public static int writeFrequencyMetricsHeader(final short choice,
                                                  final int repetition,
                                                  final long interval,
                                                  final long time,
                                                  final MutableDirectBuffer dst,
                                                  final int dstOffset) {
        dst.putByte(dstOffset + VERSION_OFFSET, VERSION);
        dst.putByte(dstOffset + FLAGS_OFFSET, FLAGS_NONE);
        dst.putShort(dstOffset + CHOICE_OFFSET, choice);
        dst.putInt(dstOffset + REPETITION_OFFSET, repetition);
        dst.putLong(dstOffset + INTERVAL_OFFSET, interval);
        dst.putLong(dstOffset + TIME_OFFSET, time);
        return HEADER_LENGTH;
    }

    public static int writeCounter(final long counter,
                                   final MutableDirectBuffer dst,
                                   final int dstOffset) {
        dst.putLong(dstOffset, counter);
        return Long.BYTES;
    }

    public static void validateVersion(final short version) {
        if (version != VERSION) {
            throw new IllegalArgumentException("Metric version " + version +
                    " is not compatible with current version " + VERSION);
        }
    }

    @Override
    public String toString() {
        if (!valid()) {
            return "FlyweightMetricsStoreEntry";
        }
        return type() == Type.TIME ?
                "FlyweightMetricsStoreEntry{" +
                        "version=" + version() +
                        ", type=" + type() +
                        ", flags=" + toBinaryString(flags()) +
                        ", index=" + index() +
                        ", source=" + source() +
                        ", sequence=" + sequence() +
                        ", time=" + time() +
                        ", count=" + count() +
                        '}' :
                "FlyweightMetricsStoreEntry{" +
                        "version=" + version() +
                        ", type=" + type() +
                        ", choice=" + toBinaryString(choice()) +
                        ", repetition=" + repetition() +
                        ", interval=" + interval() +
                        ", time=" + time() +
                        ", count=" + count() +
                        '}'
                ;

    }
}
