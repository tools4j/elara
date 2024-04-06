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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.handler.CommandSendingCommandHandler;
import org.tools4j.elara.send.CommandHandlingSender;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.source.CommandSource;

import static java.util.Objects.requireNonNull;

final class DiscardingSenderSupplier implements SenderSupplier {
    private static final int INITIAL_BUFFER_CAPACITY = 4096;//FIXME make configurable
    private final ExceptionHandler exceptionHandler;
    private final ActivationPlugin plugin;
    private final CommandHandlingSender commandHandlingSender;
    private final SenderSupplier activeSenderSupplier;
    private CommandSender activeSender;

    public DiscardingSenderSupplier(final AppConfig appConfig,
                                    final ActivationPlugin plugin,
                                    final SenderSupplier activeSenderSupplier) {
        this.exceptionHandler = appConfig.exceptionHandler();
        this.plugin = requireNonNull(plugin);
        this.activeSenderSupplier = requireNonNull(activeSenderSupplier);
        this.commandHandlingSender = new CommandHandlingSender(INITIAL_BUFFER_CAPACITY, appConfig.timeSource(), this::onCommand);
    }

    @Override
    public CommandSender senderFor(final CommandSource commandSource, final SentListener sentListener) {
        activeSender = activeSenderSupplier.senderFor(commandSource, (sourceSequence, commandTime) -> {});
        return commandHandlingSender.senderFor(commandSource, sentListener);
    }

    private void onCommand(final Command command) {
        if (activeSender == null) {
            throw new IllegalStateException("No active command sender");
        }
        if (plugin.isActive()) {
            CommandSendingCommandHandler.handleCommand(command, activeSender, exceptionHandler);
        }
        activeSender = null;
    }
}
