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
package org.tools4j.elara.plugin.replication;

import org.agrona.DirectBuffer;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.store.MessageStore.Handler;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.store.MessageStore.Handler.Result.PEEK;
import static org.tools4j.elara.store.MessageStore.Handler.Result.POLL;

public class ReplicationCommandHandler implements Handler {

    private final ReplicationPlugin plugin;
    private final BaseState baseState;
    private final ReplicationState replicationState;
    private final Handler leaderHandler;

    public ReplicationCommandHandler(final ReplicationPlugin plugin,
                                     final BaseState baseState,
                                     final ReplicationState replicationState,
                                     final Handler leaderHandler) {
        this.plugin = requireNonNull(plugin);
        this.baseState = requireNonNull(baseState);
        this.replicationState = requireNonNull(replicationState);
        this.leaderHandler = requireNonNull(leaderHandler);
    }

    @Override
    public Result onMessage(final DirectBuffer message) {
        if (plugin.isLeader(replicationState)) {
            return leaderHandler.onMessage(message);
        }
        final int sourceId = FlyweightCommand.sourceId(message);
        final long sourceSequence = FlyweightCommand.sourceSequence(message);
        if (baseState.eventAppliedForCommand(sourceId, sourceSequence)) {
            //FIXME only consider committed events here
            //TODO call handler for skipped command
            return POLL;
        }
        return PEEK;
    }
}
