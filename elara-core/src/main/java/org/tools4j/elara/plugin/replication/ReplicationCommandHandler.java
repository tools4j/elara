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

import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.handler.DefaultCommandHandler;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.store.MessageStore.Handler.Result.PEEK;
import static org.tools4j.elara.store.MessageStore.Handler.Result.POLL;

public class ReplicationCommandHandler extends DefaultCommandHandler {

    private final TimeSource timeSource;
    private final Configuration configuration;
    private final ReplicationState state;

    public ReplicationCommandHandler(final TimeSource timeSource,
                                     final Configuration configuration,
                                     final BaseState baseState,
                                     final ReplicationState state,
                                     final DefaultEventRouter eventRouter,
                                     final CommandProcessor commandProcessor,
                                     final ExceptionHandler exceptionHandler,
                                     final DuplicateHandler duplicateHandler) {
        super(baseState, eventRouter, commandProcessor, exceptionHandler, duplicateHandler);
        this.timeSource = requireNonNull(timeSource);
        this.configuration = requireNonNull(configuration);
        this.state = requireNonNull(state);
    }

    @Override
    public Result onCommand(final Command command) {
        if (allEventsAppliedFor(command)) {
            skipCommand(command);
            return POLL;
        }
        if (isLeader()) {
            processCommand(command);
            return POLL;
        }
        return PEEK;
    }

    private boolean isLeader() {
        if (state.leaderId() == configuration.serverId()) {
            final long time = timeSource.currentTime();
            if (time - state.lastAppliedEventTime() <= configuration.leaderTimeout()) {
                return true;
            }
        }
        return false;
    }
}
