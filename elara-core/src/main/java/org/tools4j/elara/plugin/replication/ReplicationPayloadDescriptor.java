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
package org.tools4j.elara.plugin.replication;

import org.agrona.DirectBuffer;

/**
 * Descriptor of payload layout for replication commands and events in a byte buffer.
 * <pre>

     0         1         2         3         4         5         6
     0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |Version| Flags |     Type      |         Data Size = 0         |
     +-------+-------+-------+-------+-------+-------+-------+-------+
     |      Candidate/Leader ID      |             Term              |
     +-------+-------+-------+-------+-------+-------+-------+-------+


 * </pre>
 */
public enum ReplicationPayloadDescriptor {
    ;
    public static final byte VERSION = 1;
    public static final byte FLAGS_NONE = 0;

    public static final int VERSION_OFFSET = 0;
    public static final int VERSION_LENGTH = Byte.BYTES;
    public static final int FLAGS_OFFSET = VERSION_OFFSET + VERSION_LENGTH;
    public static final int FLAGS_LENGTH = Byte.BYTES;
    public static final int TYPE_OFFSET = FLAGS_OFFSET + FLAGS_LENGTH;
    public static final int TYPE_LENGTH = Short.BYTES;
    public static final int DATA_SIZE_OFFSET = TYPE_OFFSET + TYPE_LENGTH;
    public static final int DATA_SIZE_LENGTH = Integer.BYTES;
    public static final int CANDIDATE_ID_OFFSET = DATA_SIZE_OFFSET + DATA_SIZE_LENGTH;
    public static final int CANDIDATE_ID_LENGTH = Integer.BYTES;
    public static final int TERM_OFFSET = CANDIDATE_ID_OFFSET + CANDIDATE_ID_LENGTH;
    public static final int TERM_LENGTH = Integer.BYTES;

    public static final int PAYLOAD_LENGTH = TERM_OFFSET + TERM_LENGTH;

    //aliases
    public static final int LEADER_ID_OFFSET = CANDIDATE_ID_OFFSET;
    public static final int LEADER_ID_LENGTH = CANDIDATE_ID_LENGTH;

    public static byte version(final DirectBuffer payload) {
        return payload.getByte(VERSION_OFFSET);
    }

    public static byte flags(final DirectBuffer payload) {
        return payload.getByte(FLAGS_OFFSET);
    }

    public static int type(final DirectBuffer payload) {
        return payload.getInt(TYPE_OFFSET);
    }

    public static int dataSize(final DirectBuffer payload) {
        return payload.getInt(DATA_SIZE_OFFSET);
    }

    public static int candidateId(final DirectBuffer payload) {
        return payload.getInt(CANDIDATE_ID_OFFSET);
    }

    public static int leaderId(final DirectBuffer payload) {
        return payload.getInt(LEADER_ID_OFFSET);
    }

    public static int term(final DirectBuffer payload) {
        return payload.getInt(TERM_OFFSET);
    }
}
