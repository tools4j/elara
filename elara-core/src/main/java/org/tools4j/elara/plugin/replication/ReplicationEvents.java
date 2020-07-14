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

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.Frame;

import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.LEADER_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TERM_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION_OFFSET;

/**
 * Replication events.
 */
public enum ReplicationEvents {
    ;
    public static final short LEADER_ELECTED = -90;
    public static final short LEADER_ENFORCED = -99;

    public static int leaderElected(final MutableDirectBuffer buffer, final int offset,
                                    final int term,
                                    final int leaderId) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_ELECTED);
        buffer.putInt(offset + PAYLOAD_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int leaderEnforced(final MutableDirectBuffer buffer, final int offset,
                                     final int term,
                                     final int leaderId) {
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_ENFORCED);
        buffer.putInt(offset + PAYLOAD_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int term(final Event event) {
        return ReplicationPayloadDescriptor.term(event.payload());
    }

    public static int leaderId(final Event event) {
        return ReplicationPayloadDescriptor.leaderId(event.payload());
    }

    public static boolean isReplicationEvent(final Event event) {
        return isReplicationEventType(event.type());
    }

    public static boolean isReplicationEvent(final Frame frame) {
        return frame.header().index() >= 0 && isReplicationEventType(frame.header().type());
    }

    public static boolean isReplicationEventType(final int eventType) {
        switch (eventType) {
            case LEADER_ELECTED://fallthrough
            case LEADER_ENFORCED://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String replicationEventName(final Event event) {
        return replicationEventName(event.type());
    }

    public static String replicationEventName(final Frame frame) {
        return replicationEventName(frame.header().type());
    }

    public static String replicationEventName(final int eventType) {
        switch (eventType) {
            case LEADER_ELECTED:
                return "LEADER_ELECTED";
            case LEADER_ENFORCED:
                return "LEADER_ENFORCED";
            default:
                throw new IllegalArgumentException("Not a replication event type: " + eventType);
        }
    }
}