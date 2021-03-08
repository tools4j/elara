/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.command.Command;
import org.tools4j.elara.flyweight.Frame;

import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.CANDIDATE_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.LEADER_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.DATA_SIZE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TERM_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION_OFFSET;

/**
 * Replication commands.
 */
public enum ReplicationCommands {
    ;
    /**
     * Command issued to trigger a LEADER_HEARTBEAT event.
     */
    public static final short TRIGGER_HEARTBEAT = -90;
    /**
     * Command to propose a new leader.
     */
    public static final short PROPOSE_LEADER = -91;

    public static int triggerHeartbeat(final MutableDirectBuffer buffer,
                                       final int offset,
                                       final int term,
                                       final int leaderId) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, TRIGGER_HEARTBEAT);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
        buffer.putInt(offset + LEADER_ID_OFFSET, leaderId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int proposeLeader(final MutableDirectBuffer buffer,
                                    final int offset,
                                    final int term,
                                    final int candidateId) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, PROPOSE_LEADER);
        buffer.putInt(offset + DATA_SIZE_OFFSET, 0);
        buffer.putInt(offset + CANDIDATE_ID_OFFSET, candidateId);
        buffer.putInt(offset + TERM_OFFSET, term);
        return PAYLOAD_LENGTH;
    }

    public static int candidateId(final Command command) {
        return ReplicationPayloadDescriptor.candidateId(command.payload());
    }

    public static boolean isReplicationCommand(final Command command) {
        return isReplicationCommandType(command.type());
    }

    public static boolean isReplicationCommand(final Frame frame) {
        return frame.header().index() >= 0 && isReplicationCommandType(frame.header().type());
    }

    public static boolean isReplicationCommandType(final int commandType) {
        switch (commandType) {
            case PROPOSE_LEADER://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String replicationCommandName(final Command command) {
        return replicationCommandName(command.type());
    }

    public static String replicationCommandName(final Frame frame) {
        return replicationCommandName(frame.header().type());
    }

    public static String replicationCommandName(final int commandType) {
        switch (commandType) {
            case PROPOSE_LEADER:
                return "PROPOSE_LEADER";
            default:
                throw new IllegalArgumentException("Not a replication command type: " + commandType);
        }
    }
}
