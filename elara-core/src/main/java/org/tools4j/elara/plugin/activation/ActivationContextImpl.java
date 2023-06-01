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
package org.tools4j.elara.plugin.activation;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.type.AppType;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.StoreAppendingMessageSender;
import org.tools4j.elara.store.StorePollingMessageReceiver;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.ipc.SharedBuffer;

import static java.util.Objects.requireNonNull;

public class ActivationContextImpl implements ActivationContext {

    private CommandReplayMode commandReplayMode = CommandReplayMode.DISCARD;
    private MessageSender commandCacheSender;
    private MessageReceiver commandCacheReceiver;

    @Override
    public CommandReplayMode commandReplayMode() {
        return commandReplayMode;
    }

    @Override
    public ActivationContext commandReplayMode(final CommandReplayMode mode) {
        this.commandReplayMode = requireNonNull(mode);
        return this;
    }

    @Override
    public MessageSender commandCacheSender() {
        return commandCacheSender;
    }

    @Override
    public MessageReceiver commandCacheReceiver() {
        return commandCacheReceiver;
    }

    @Override
    public ActivationContext commandCache(final MessageStore commandStore) {
        commandCacheSender = new StoreAppendingMessageSender(commandStore);
        commandCacheReceiver = new StorePollingMessageReceiver(commandStore);
        return this;
    }

    @Override
    public ActivationContext commandCache(final SharedBuffer commandBuffer) {
        commandCacheSender = commandBuffer.sender();
        commandCacheReceiver = commandBuffer.receiver();
        return this;
    }

    @Override
    public ActivationContext commandCacheSender(final MessageSender sender) {
        this.commandCacheSender = requireNonNull(sender);
        return this;
    }

    @Override
    public ActivationContext commandCacheReceiver(final MessageReceiver receiver) {
        this.commandCacheReceiver = requireNonNull(receiver);
        return this;
    }

    @Override
    public void validate(final AppConfig appConfig) {
        final AppType appType = appConfig.appType();
        final CommandReplayMode replayMode = commandReplayMode();
        if (replayMode == CommandReplayMode.REPLAY) {
            if (appType.isSequencerApp() || appType.isProcessorApp()) {
                if (!hasCommandStore(appConfig)) {
                    throw new IllegalArgumentException("Sequencer or processor app must have command store if command " +
                            CommandReplayMode.REPLAY + " mode is set");
                }
            } else {
                if (commandCacheSender == null || commandCacheReceiver == null) {
                    throw new IllegalArgumentException("Command cache sender and receiver must be defined if command " +
                            CommandReplayMode.REPLAY + " mode is set");
                }
            }
        }
    }

    static boolean hasCommandStore(final AppConfig appConfig) {
        return appConfig.appType().hasCommandStore(appConfig);
    }
}
