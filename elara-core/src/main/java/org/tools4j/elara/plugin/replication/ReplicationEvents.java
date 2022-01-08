/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.CANDIDATE_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.DATA_SIZE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.LEADER_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TERM_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION_OFFSET;

/**
 * Replication events.
 */
public enum ReplicationEvents {
    ;
    /**
     * A leader has triggered a heartbeat event to prevent timeouts.
     */
    public static final short LEADER_HEARTBEAT = -90;
    /**
     * Leader change occurred and a new term has started in response to a
     * {@link org.tools4j.elara.plugin.replication.ReplicationCommands#PROPOSE_LEADER PROPOSE_LEADER} command
     */
    public static final short LEADER_ELECTED = -91;
    /**
     * A leader was proposed or enforced but was already leader at the time or processing the command or input.
     */
    public static final short LEADER_CONFIRMED = -92;
    /**
     * A leader was proposed or attempted to be enforced but was rejected because the candidate ID was invalid.
     */
    public static final short LEADER_REJECTED = -93;
    /**
     * Leader change occurred and a new term has started in response an enforce-leader input received via
     * {@link EnforceLeaderInput}.
     */
    public static final short LEADER_ENFORCED = -99;

    public static int leaderHeartbeat(final MutableDirectBuffer buffer, final int offset,
                                      final int term,
                                      final int leaderId) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_HEARTBEAT);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int leaderElected(final MutableDirectBuffer buffer, final int offset,
                                    final int term,
                                    final int leaderId) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_ELECTED);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int leaderConfirmed(final MutableDirectBuffer buffer, final int offset,
                                      final int term,
                                      final int leaderId) {
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_CONFIRMED);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int leaderRejected(final MutableDirectBuffer buffer, final int offset,
                                     final int term,
                                     final int candidateId) {
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_REJECTED);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
        buffer.putInt(offset + CANDIDATE_ID_OFFSET, candidateId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int leaderEnforced(final MutableDirectBuffer buffer, final int offset,
                                     final int term,
                                     final int leaderId) {
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, LEADER_ENFORCED);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
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
            case LEADER_CONFIRMED://fallthrough
            case LEADER_REJECTED://fallthrough
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
            case LEADER_CONFIRMED:
                return "LEADER_CONFIRMED";
            case LEADER_REJECTED:
                return "LEADER_REJECTED";
            case LEADER_ENFORCED:
                return "LEADER_ENFORCED";
            default:
                throw new IllegalArgumentException("Not a replication event type: " + eventType);
        }
    }
}
