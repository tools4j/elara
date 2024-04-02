/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.plugin.activation;

import org.agrona.DirectBuffer;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.store.MessageStore.Handler;

import static java.util.Objects.requireNonNull;

final class ActivationCommandPollerHandler implements Handler {

    private final ActivationPlugin plugin;
    private final EventApplicationState eventApplicationState;
    private final CommandHandler commandHandler;
    private final FlyweightCommand flyweightCommand = new FlyweightCommand();

    public ActivationCommandPollerHandler(final ActivationPlugin plugin,
                                          final BaseState baseState,
                                          final CommandHandler commandHandler) {
        this.plugin = requireNonNull(plugin);
        this.eventApplicationState = EventApplicationState.create(baseState);
        this.commandHandler = requireNonNull(commandHandler);
    }

    @Override
    public Result onMessage(final DirectBuffer message) {
        flyweightCommand.wrap(message, 0);
        try {
            if (plugin.isActive()) {
                commandHandler.onCommand(flyweightCommand);
                return Result.POLL;
            }
            if (plugin.config().commandReplayMode() == CommandReplayMode.DISCARD ||
                    eventApplicationState.allEventsAppliedFor(flyweightCommand)) {
                return Result.POLL;
            }
            return Result.PEEK;
        } finally {
            flyweightCommand.reset();
        }
    }
}
