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

import org.agrona.DirectBuffer;

/**
 * Descriptor of frame layout for time and frequency counter metrics.
 * <pre>

                           Time metrics frame

     0         1         2         3         4         5         6
     0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |Version| Flags |     Index     |            Source             |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                           Sequence                            |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                             Time                              |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                            Time 0                             |
     |                            Time 1                             |
     |                             ...                               |


                     Frequency counter metrics frame

     0         1         2         3         4         5         6
     0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |Version|Flags=0|    Choice     |          Repetition           |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                           Interval                            |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                             Time                              |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                            Count 0                            |
     |                            Count 1                            |
     |                             ...                               |

 * </pre>
 */
public enum MetricsDescriptor {
    ;

    public static final byte VERSION = 2;

    static final byte FLAGS_NONE = 0;

    public static final int VERSION_OFFSET = 0;
    public static final int VERSION_LENGTH = Byte.BYTES;
    public static final int FLAGS_OFFSET = VERSION_OFFSET + VERSION_LENGTH;
    public static final int FLAGS_LENGTH = Byte.BYTES;
    public static final int INDEX_OFFSET = FLAGS_OFFSET + FLAGS_LENGTH;
    public static final int INDEX_LENGTH = Short.BYTES;
    public static final int SOURCE_OFFSET = INDEX_OFFSET + INDEX_LENGTH;
    public static final int SOURCE_LENGTH = Integer.BYTES;
    public static final int SEQUENCE_OFFSET = SOURCE_OFFSET + SOURCE_LENGTH;
    public static final int SEQUENCE_LENGTH = Long.BYTES;
    public static final int TIME_OFFSET = SEQUENCE_OFFSET + SEQUENCE_LENGTH;
    public static final int TIME_LENGTH = Long.BYTES;

    public static final int CHOICE_OFFSET = FLAGS_OFFSET + FLAGS_LENGTH;
    public static final int CHOICE_LENGTH = Short.BYTES;
    public static final int REPETITION_OFFSET = CHOICE_OFFSET + CHOICE_LENGTH;
    public static final int REPETITION_LENGTH = Integer.BYTES;
    public static final int INTERVAL_OFFSET = REPETITION_OFFSET + REPETITION_LENGTH;
    public static final int INTERVAL_LENGTH = Long.BYTES;

    public static final int HEADER_OFFSET = 0;
    public static final int HEADER_LENGTH = TIME_OFFSET + TIME_LENGTH;

    public static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_LENGTH;

    public static byte version(final DirectBuffer buffer) {
        return buffer.getByte(VERSION_OFFSET);
    }

    public static byte flags(final DirectBuffer buffer) {
        return buffer.getByte(FLAGS_OFFSET);
    }

    public static boolean isTimeMetrics(final DirectBuffer buffer) {
        return FLAGS_NONE != flags(buffer);
    }

    public static boolean isFrequencyMetrics(final DirectBuffer buffer) {
        return FLAGS_NONE == flags(buffer);
    }

    public static short index(final DirectBuffer buffer) {
        return buffer.getShort(INDEX_OFFSET);
    }

    public static int source(final DirectBuffer buffer) {
        return buffer.getInt(SOURCE_OFFSET);
    }

    public static long sequence(final DirectBuffer buffer) {
        return buffer.getLong(SEQUENCE_OFFSET);
    }

    public static long time(final DirectBuffer buffer) {
        return buffer.getLong(TIME_OFFSET);
    }

    public static short choice(final DirectBuffer buffer) {
        return buffer.getShort(CHOICE_OFFSET);
    }

    public static int repetition(final DirectBuffer buffer) {
        return buffer.getInt(REPETITION_OFFSET);
    }

    public static long interval(final DirectBuffer buffer) {
        return buffer.getInt(INTERVAL_OFFSET);
    }

    public static long time(int index, final DirectBuffer buffer) {
        return buffer.getLong(PAYLOAD_OFFSET + index * Long.BYTES);
    }

    public static long counter(int index, final DirectBuffer buffer) {
        return buffer.getLong(PAYLOAD_OFFSET + index * Long.BYTES);
    }

}
