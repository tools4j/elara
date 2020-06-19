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

import org.tools4j.elara.event.Event;

/**
 * Boot events issued in response to boot commands.
 */
public enum ReplicationEvents {
    ;
    public static final int LEADER_VOTE_REQUESTED = -90;
    public static final int LEADER_VOTE_GRANTED = -91;
    public static final int LEADER_ELECTED = -92;
    public static final int LEADER_ENFORCED = -93;

    public static final int EVENT_APPENDED = -94;

    public static boolean isBootEvent(final Event event) {
        switch (event.type()) {
            case LEADER_VOTE_REQUESTED://fallthrough
            case LEADER_VOTE_GRANTED://fallthrough
            case LEADER_ELECTED://fallthrough
            case LEADER_ENFORCED://fallthrough
            case EVENT_APPENDED://fallthrough
                return true;
            default:
                return false;
        }
    }

    public static String replicationEventName(final Event event) {
        switch (event.type()) {
            case LEADER_VOTE_REQUESTED:
                return "LEADER_VOTE_REQUESTED";
            case LEADER_VOTE_GRANTED:
                return "LEADER_VOTE_GRANTED";
            case LEADER_ELECTED:
                return "LEADER_ELECTED";
            case LEADER_ENFORCED:
                return "LEADER_ENFORCED";
            case EVENT_APPENDED:
                return "EVENT_APPENDED";
            default:
                throw new IllegalArgumentException("Not a boot event: " + event);
        }
    }
}
