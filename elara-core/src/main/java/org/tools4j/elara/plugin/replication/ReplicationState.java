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

import org.tools4j.elara.event.Event;

public interface ReplicationState {
    int NULL_SERVER = -1;

    int term();
    int leaderId();
    long lastAppliedEventTime();

    interface Volatile extends ReplicationState {
        long eventLogSize();
        long nextEventLogIndex(int serverId);
        long confirmedEventLogIndex(int serverId);
        long committedEventLogIndex(int serverCount);
        long nextNotBefore(int serverId);

        Volatile nextEventLogIndex(int serverId, long index);
        Volatile confirmedEventLogIndex(int serverId, long index);
        Volatile nextNotBefore(int serverId, long time);
    }

    interface Mutable extends Volatile {
        Mutable term(int term);
        Mutable leaderId(int leaderId);
        Mutable eventApplied(Event event);
    }
}
