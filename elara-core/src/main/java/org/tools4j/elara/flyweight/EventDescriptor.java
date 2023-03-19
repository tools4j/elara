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

/**
 * Descriptor of event layout in a byte buffer:
 * <pre>

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=ET|     Index     |          Frame Size           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |           Source ID           |         Payload Type          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Source Sequence                        |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Event Sequence                         |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                          Event Time                           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                            Payload                            |
    |                             ...                               |

    ET = 02-04, see {@link EventType#frameType()}

 * </pre>
 *
 * @see FrameDescriptor
 */
public enum EventDescriptor {
    ;
    public static final int INDEX_OFFSET = FrameDescriptor.RESERVED_OFFSET;

    public static final int INDEX_LENGTH = FrameDescriptor.RESERVED_LENGTH;
    public static final int SOURCE_ID_OFFSET = FrameDescriptor.HEADER_LENGTH;
    public static final int SOURCE_ID_LENGTH = Integer.BYTES;

    public static final int PAYLOAD_TYPE_OFFSET = SOURCE_ID_OFFSET + SOURCE_ID_LENGTH;
    public static final int PAYLOAD_TYPE_LENGTH = Integer.BYTES;
    public static final int SOURCE_SEQUENCE_OFFSET = PAYLOAD_TYPE_OFFSET + PAYLOAD_TYPE_LENGTH;
    public static final int SOURCE_SEQUENCE_LENGTH = Long.BYTES;
    public static final int EVENT_SEQUENCE_OFFSET = SOURCE_SEQUENCE_OFFSET + SOURCE_SEQUENCE_LENGTH;
    public static final int EVENT_SEQUENCE_LENGTH = Long.BYTES;
    public static final int EVENT_TIME_OFFSET = EVENT_SEQUENCE_OFFSET + EVENT_SEQUENCE_LENGTH;
    public static final int EVENT_TIME_LENGTH = Long.BYTES;

    public static final int HEADER_OFFSET = FrameDescriptor.HEADER_OFFSET;
    public static final int HEADER_LENGTH = EVENT_TIME_OFFSET + EVENT_TIME_LENGTH;

    public static final int PAYLOAD_OFFSET = HEADER_LENGTH;
}
