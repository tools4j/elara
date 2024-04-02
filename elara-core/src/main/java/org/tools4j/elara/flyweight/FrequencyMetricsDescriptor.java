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
package org.tools4j.elara.flyweight;

/**
 * Descriptor of frame layout for frequency metrics.
 * <pre>

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=07| Metric Types  |          Frame Size           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                           Iteration                           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                           Interval                            |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                          Metric Time                          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                            Count 0                            |
    |                            Count 1                            |
    |                             ...                               |

 * </pre>
 *
 * @see TimeMetricsDescriptor
 * @see FrameDescriptor
 */
public enum FrequencyMetricsDescriptor {
    ;

    public static final int ITERATION_OFFSET = FrameDescriptor.HEADER_LENGTH;

    public static final int METRIC_TYPES_OFFSET = FrameDescriptor.RESERVED_OFFSET;
    public static final int METRIC_TYPES_LENGTH = FrameDescriptor.RESERVED_LENGTH;
    public static final int ITERATION_LENGTH = Long.BYTES;
    public static final int INTERVAL_OFFSET = ITERATION_OFFSET + ITERATION_LENGTH;
    public static final int INTERVAL_LENGTH = Long.BYTES;
    public static final int METRIC_TIME_OFFSET = INTERVAL_OFFSET + INTERVAL_LENGTH;
    public static final int METRIC_TIME_LENGTH = Long.BYTES;

    public static final int HEADER_OFFSET = FrameDescriptor.HEADER_OFFSET;
    public static final int HEADER_LENGTH = METRIC_TIME_OFFSET + METRIC_TIME_LENGTH;

    public static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_LENGTH;

}
