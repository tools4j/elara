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
import org.agrona.MutableDirectBuffer;

import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.CANDIDATE_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.COMMITTED_LOG_INDEX_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_APPEND_SUCCESS;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.LEADER_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.LOG_INDEX_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.TERM_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.VERSION_OFFSET;

/**
 * Replication messages exchanged by different nodes; they are neither commands nor events, that is, they are plugin
 * communication messages that are not part of the event sourcing logic.
 */
public enum ReplicationMessages {
    ;
    public static final short APPEND_REQUEST = -95;
    public static final short APPEND_RESPONSE = -96;

    public static String replicationMessageName(final int type) {
        switch (type) {
            case APPEND_REQUEST:
                return "APPEND_REQUEST";
            case APPEND_RESPONSE:
                return "APPEND_RESPONSE";
            default:
                throw new IllegalArgumentException("Not a replication message type: " + type);
        }
    }

    public static int appendRequest(final MutableDirectBuffer buffer, final int offset,
                                    final int term,
                                    final int leaderId,
                                    final long logIndex,
                                    final DirectBuffer payload,
                                    final int payloadOffset,
                                    final int payloadSize) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, APPEND_REQUEST);
        buffer.putInt(offset + PAYLOAD_SIZE_OFFSET, payloadSize);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        buffer.putLong(offset + LOG_INDEX_OFFSET, logIndex);
        buffer.putLong(offset + COMMITTED_LOG_INDEX_OFFSET, 0);//TODO commit log
        buffer.putBytes(offset + PAYLOAD_OFFSET, payload, payloadOffset, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    public static int appendResponse(final MutableDirectBuffer buffer, final int offset,
                                     final int term,
                                     final int leaderId,
                                     final long nextEventLogIndex,
                                     final boolean success) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, success ? FLAGS_APPEND_SUCCESS : FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, APPEND_RESPONSE);
        buffer.putInt(offset + PAYLOAD_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        buffer.putLong(offset + LOG_INDEX_OFFSET, nextEventLogIndex);
        buffer.putLong(offset + COMMITTED_LOG_INDEX_OFFSET, 0);//TODO commit log
        return HEADER_LENGTH;
    }

    public static byte version(final DirectBuffer buffer) {
        return buffer.getByte(VERSION_OFFSET);
    }

    public static boolean isAppendSuccess(final DirectBuffer buffer) {
        return (FLAGS_APPEND_SUCCESS & buffer.getByte(FLAGS_OFFSET)) != 0;
    }

    public static byte type(final DirectBuffer buffer) {
        return buffer.getByte(TYPE_OFFSET);
    }

    public static int candidateId(final DirectBuffer buffer) {
        return buffer.getInt(CANDIDATE_ID_OFFSET);
    }

    public static int leaderId(final DirectBuffer buffer) {
        return buffer.getInt(LEADER_ID_OFFSET);
    }

    public static int term(final DirectBuffer buffer) {
        return buffer.getInt(TERM_OFFSET);
    }

    public static int logIndex(final DirectBuffer buffer) {
        return buffer.getInt(LOG_INDEX_OFFSET);
    }

    public static int committedLogIndex(final DirectBuffer buffer) {
        return buffer.getInt(COMMITTED_LOG_INDEX_OFFSET);
    }

    public static int payloadSize(final DirectBuffer buffer) {
        return buffer.getInt(PAYLOAD_SIZE_OFFSET);
    }
}
