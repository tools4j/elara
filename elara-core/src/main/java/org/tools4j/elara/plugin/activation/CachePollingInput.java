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
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.handler.CommandSendingCommandHandler;
import org.tools4j.elara.input.MultiSourceInput;
import org.tools4j.elara.source.SourceContextProvider;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import static java.util.Objects.requireNonNull;

final class CachePollingInput implements MultiSourceInput {
    private static final int DEFAULT_INITIAL_COMMAND_CACHE_CAPACITY = 4096;//FIXME configurable
    private final ActivationPlugin plugin;
    private final EventApplicationState eventApplicationState;
    private final MessageReceiver messageReceiver;
    private final ExceptionHandler exceptionHandler;
    private final MutableDirectBuffer commandCache;
    private final Handler handler = this::onMessage;
    private final FlyweightCommand command = new FlyweightCommand();

    private SourceContextProvider sourceContextProvider;

    public CachePollingInput(final ActivationPlugin plugin, final BaseState baseState, final ExceptionHandler exceptionHandler) {
        this.plugin = requireNonNull(plugin);
        this.eventApplicationState = EventApplicationState.create(baseState);
        this.messageReceiver = requireNonNull(plugin.config().commandCacheReceiver());
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.commandCache = new ExpandableDirectByteBuffer(DEFAULT_INITIAL_COMMAND_CACHE_CAPACITY);
    }

    @Override
    public int poll(final SourceContextProvider sourceContextProvider) {
        requireNonNull(sourceContextProvider);
        if (this.sourceContextProvider != sourceContextProvider) {
            this.sourceContextProvider = sourceContextProvider;
        }
        if (command.valid()) {
            return handleCachedCommand() ? 1 : 0;
        }
        return messageReceiver.poll(handler);
    }

    private boolean handleCachedCommand() {
        if (handleCommand(command)) {
            command.reset();
            return true;
        }
        return false;
    }

    private void onMessage(final DirectBuffer message) {
        try {
            command.wrap(message, 0);
            if (handleCommand(command)) {
                command.reset();
            } else {
                //cache
                command.writeTo(commandCache, 0);
                command.wrapSilently(commandCache, 0);
            }
        } catch (final Exception e) {
            exceptionHandler.handleException("Unhandled exception when receiving command input message", command, e);
        }
    }

    private boolean handleCommand(final Command command) {
        if (plugin.isActive()) {
            CommandSendingCommandHandler.handleCommand(command, sourceContextProvider, exceptionHandler);
            return true;
        }
        return eventApplicationState.allEventsAppliedFor(command);
    }
}
