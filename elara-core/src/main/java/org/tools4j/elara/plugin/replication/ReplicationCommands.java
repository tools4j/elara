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
import org.tools4j.elara.command.Command;

import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_NONE;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationMessageDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.CANDIDATE_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.LEADER_ID_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.PAYLOAD_LENGTH;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.TERM_OFFSET;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION;
import static org.tools4j.elara.plugin.replication.ReplicationPayloadDescriptor.VERSION_OFFSET;

/**
 * Replication commands.
 */
public enum ReplicationCommands {
    ;
    public static final short PROPOSE_LEADER = -90;
    public static final short ENFORCE_LEADER = -99;//not a proper command as it is directly injected as an event

    public static boolean isReplicationCommand(final Command command) {
        switch (command.type()) {
            case PROPOSE_LEADER://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static int proposeLeader(final MutableDirectBuffer buffer, final int offset, final short candidateId) {
        return proposeOrEnforceLeader(buffer, offset, PROPOSE_LEADER, candidateId);
    }

    public static int enforceLeader(final MutableDirectBuffer buffer, final int offset, final short candidateId) {
        return proposeOrEnforceLeader(buffer, offset, ENFORCE_LEADER, candidateId);
    }

    private static int proposeOrEnforceLeader(final MutableDirectBuffer buffer, final int offset,
                                              final short type, final int candidateId) {
        buffer.putByte(offset + VERSION_OFFSET, VERSION);
        buffer.putByte(offset + FLAGS_OFFSET, FLAGS_NONE);
        buffer.putShort(offset + TYPE_OFFSET, type);
        buffer.putInt(offset + PAYLOAD_SIZE_OFFSET, 0);
        buffer.putInt(offset + CANDIDATE_ID_OFFSET, candidateId);
        buffer.putInt(offset + TERM_OFFSET, 0);
        return PAYLOAD_LENGTH;
    }

    public static int candidateId(final Command command) {
        return command.payload().getInt(CANDIDATE_ID_OFFSET);
    }

    public static int leaderId(final DirectBuffer buffer) {
        return buffer.getInt(LEADER_ID_OFFSET);
    }
}
