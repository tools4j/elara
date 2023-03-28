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
 * Descriptor of byte layouts of Elara data in a byte buffer.  The exact layout depends on the
 * {@link FrameType}, but every frame starts with a header possibly followed by user defined
 * payload data.
 * <p>
 * <br>
 * Elara frames types are defined as follows:
 * </p>
 *
 * <pre>

    General Header common to all frame types:

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version| Type  |   Reserved    |          Frame Size           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Type Dependent                         |
    |                             ...                               |


    Command Header: Type=1

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=01|   Reserved    |          Frame Size           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |           Source ID           |         Payload Type          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Source Sequence                        |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                         Command Time                          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                            Payload                            |
    |                             ...                               |


    Intermediary Event Header: Type=2

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=02|  Event Index  |          Frame Size           |
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


    Commit Event Header: Type=3

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=03|  Event Index  |          Frame Size           |
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


    Auto-Commit Event Header: Type=4

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=04|  Event Index  |       Frame Size = 40         |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |           Source ID           |         Payload Type          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Source Sequence                        |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Event Sequence                         |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                          Event Time                           |
    +-------+-------+-------+-------+-------+-------+-------+-------+


    Rollback Event Header: Type=5

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=05|  Event Index  |       Frame Size = 40         |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |           Source ID           |         Payload Type          |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Source Sequence                        |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                        Event Sequence                         |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |                          Event Time                           |
    +-------+-------+-------+-------+-------+-------+-------+-------+


    Time Metric Header: Type=6

    0         1         2         3         4         5         6
    0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |Version|Type=06|  Event Index  |          Frame Size           |
    +-------+-------+-------+-------+-------+-------+-------+-------+
    |           Source ID           |EO       Metric Types          |
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


    Frequency Metric Header: Type=7

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
 * @see FrameType
 * @see CommandDescriptor
 * @see EventDescriptor
 * @see TimeMetricsDescriptor
 * @see FrequencyMetricsDescriptor
 */
public enum FrameDescriptor {
    ;

    public static final int VERSION_OFFSET = 0;
    public static final int VERSION_LENGTH = Byte.BYTES;
    public static final int TYPE_OFFSET = VERSION_OFFSET + VERSION_LENGTH;
    public static final int TYPE_LENGTH = Byte.BYTES;
    public static final int RESERVED_OFFSET = TYPE_OFFSET + TYPE_LENGTH;

    public static final int RESERVED_LENGTH = Short.BYTES;
    public static final int FRAME_SIZE_OFFSET = RESERVED_OFFSET + RESERVED_LENGTH;
    public static final int FRAME_SIZE_LENGTH = Integer.BYTES;

    public static final int HEADER_OFFSET = 0;
    public static final int HEADER_LENGTH = FRAME_SIZE_OFFSET + FRAME_SIZE_LENGTH;
}
