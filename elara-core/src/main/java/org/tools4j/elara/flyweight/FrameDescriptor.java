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
 * Descriptor of frame layout for commands and events in a byte buffer.  A frame
 * starts with a header followed by payload data:
 * <pre>

     0         1         2         3         4         5         6
     0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |            Source             |             Type              |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                           Sequence                            |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                             Time                              |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |Version| Flags |     Index     |         Payload Size          |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |                           Payload                             |
     |                             ...                               |

 * </pre>
 */
public enum FrameDescriptor {
    ;

    public static final int SOURCE_OFFSET = 0;
    public static final int SOURCE_LENGTH = Integer.BYTES;
    public static final int TYPE_OFFSET = SOURCE_OFFSET + SOURCE_LENGTH;
    public static final int TYPE_LENGTH = Integer.BYTES;
    public static final int SEQUENCE_OFFSET = TYPE_OFFSET + TYPE_LENGTH;
    public static final int SEQUENCE_LENGTH = Long.BYTES;
    public static final int TIME_OFFSET = SEQUENCE_OFFSET + SEQUENCE_LENGTH;
    public static final int TIME_LENGTH = Long.BYTES;
    public static final int VERSION_OFFSET = TIME_OFFSET + TIME_LENGTH;
    public static final int VERSION_LENGTH = Byte.BYTES;
    public static final int FLAGS_OFFSET = VERSION_OFFSET + VERSION_LENGTH;
    public static final int FLAGS_LENGTH = Byte.BYTES;
    public static final int INDEX_OFFSET = FLAGS_OFFSET + FLAGS_LENGTH;
    public static final int INDEX_LENGTH = Short.BYTES;
    public static final int PAYLOAD_SIZE_OFFSET = INDEX_OFFSET + INDEX_LENGTH;
    public static final int PAYLOAD_SIZE_LENGTH = Integer.BYTES;

    public static final int HEADER_OFFSET = 0;
    public static final int HEADER_LENGTH = PAYLOAD_SIZE_OFFSET + PAYLOAD_SIZE_LENGTH;

    public static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_LENGTH;
}
