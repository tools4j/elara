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

import org.agrona.MutableDirectBuffer;

import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.CHOICE_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.REPETITION_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.SEQUENCE_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.SOURCE_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.TIME_OFFSET;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.VERSION;
import static org.tools4j.elara.plugin.metrics.MetricsDescriptor.VERSION_OFFSET;

public enum MetricsLogEntry {
    ;

    public static int writeTimeMetricsHeader(final byte flags,
                                             final short index,
                                             final int source,
                                             final long sequence,
                                             final MutableDirectBuffer dst,
                                             final int dstOffset) {
        dst.putByte(dstOffset + VERSION_OFFSET, VERSION);
        dst.putByte(dstOffset + FLAGS_OFFSET, flags);
        dst.putShort(dstOffset + INDEX_OFFSET, index);
        dst.putInt(dstOffset + SOURCE_OFFSET, source);
        dst.putLong(dstOffset + SEQUENCE_OFFSET, sequence);
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
                                                  final long time,
                                                  final MutableDirectBuffer dst,
                                                  final int dstOffset) {
        dst.putByte(dstOffset + VERSION_OFFSET, VERSION);
        dst.putByte(dstOffset + FLAGS_OFFSET, FLAGS_NONE);
        dst.putShort(dstOffset + CHOICE_OFFSET, choice);
        dst.putInt(dstOffset + REPETITION_OFFSET, repetition);
        dst.putLong(dstOffset + TIME_OFFSET, time);
        return HEADER_LENGTH;
    }

    public static int writeCounter(final long counter,
                                   final MutableDirectBuffer dst,
                                   final int dstOffset) {
        dst.putLong(dstOffset, counter);
        return Long.BYTES;
    }

}
