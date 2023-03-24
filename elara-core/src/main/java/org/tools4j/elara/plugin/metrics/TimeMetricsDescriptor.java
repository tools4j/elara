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
package org.tools4j.elara.plugin.metrics;

import org.tools4j.elara.flyweight.EventDescriptor;
import org.tools4j.elara.flyweight.FrameDescriptor;

/**
 * Descriptor of frame layout for time metrics.
 * <pre>

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=06|  Event Index  |          Frame Size           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |           Source ID           |EO      Metric Types           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Source Sequence                        |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Event Sequence                         |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                          Metric Time                          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                            Time 0                             |
    |                            Time 1                             |
    |                             ...                               |

    E = Event Flag, O = Output Flag, Target: COMMAND(00), EVENT(10), OUTPUT(11)

 * </pre>
 *
 * @see FrequencyMetricsDescriptor
 * @see FrameDescriptor
 */
public enum TimeMetricsDescriptor {
    ;

    static final byte FLAGS_NONE = 0;

    public static final int EVENT_INDEX_OFFSET = FrameDescriptor.RESERVED_OFFSET;

    public static final int EVENT_INDEX_LENGTH = FrameDescriptor.RESERVED_LENGTH;
    public static final int SOURCE_ID_OFFSET = EventDescriptor.SOURCE_ID_OFFSET;
    public static final int SOURCE_ID_LENGTH = EventDescriptor.SOURCE_ID_LENGTH;

    public static final int METRIC_TYPES_OFFSET = EventDescriptor.PAYLOAD_TYPE_OFFSET;
    public static final int METRIC_TYPES_LENGTH = EventDescriptor.PAYLOAD_TYPE_LENGTH;
    public static final int SOURCE_SEQUENCE_OFFSET = EventDescriptor.SOURCE_SEQUENCE_OFFSET;
    public static final int SOURCE_SEQUENCE_LENGTH = EventDescriptor.SOURCE_SEQUENCE_LENGTH;
    public static final int EVENT_SEQUENCE_OFFSET = EventDescriptor.EVENT_SEQUENCE_OFFSET;
    public static final int EVENT_SEQUENCE_LENGTH = EventDescriptor.EVENT_SEQUENCE_LENGTH;
    public static final int METRIC_TIME_OFFSET = EVENT_SEQUENCE_OFFSET + EVENT_SEQUENCE_LENGTH;
    public static final int METRIC_TIME_LENGTH = Long.BYTES;

    public static final int HEADER_OFFSET = FrameDescriptor.HEADER_OFFSET;
    public static final int HEADER_LENGTH = METRIC_TIME_OFFSET + METRIC_TIME_LENGTH;

    public static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_LENGTH;
}
