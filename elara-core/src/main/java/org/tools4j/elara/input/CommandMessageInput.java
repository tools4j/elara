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
package org.tools4j.elara.input;

import org.agrona.DirectBuffer;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.handler.CommandSendingCommandHandler;
import org.tools4j.elara.source.SourceContextProvider;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import static java.util.Objects.requireNonNull;

public class CommandMessageInput implements MultiSourceInput {

    private final MessageReceiver messageReceiver;
    private final ExceptionHandler exceptionHandler;
    private final Handler handler = this::onMessage;
    private final FlyweightCommand command = new FlyweightCommand();

    private SourceContextProvider sourceContextProvider;


    public CommandMessageInput(final MessageReceiver messageReceiver, final ExceptionHandler exceptionHandler) {
        this.messageReceiver = requireNonNull(messageReceiver);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public int poll(final SourceContextProvider sourceContextProvider) {
        requireNonNull(sourceContextProvider);
        if (this.sourceContextProvider != sourceContextProvider) {
            this.sourceContextProvider = sourceContextProvider;
        }
        return messageReceiver.poll(handler);
    }

    private void onMessage(final DirectBuffer message) {
        try {
            command.wrap(message, 0);
            CommandSendingCommandHandler.handleCommand(command, sourceContextProvider, exceptionHandler);
        } catch (final Exception e) {
            exceptionHandler.handleException("Unhandled exception when receiving command input message", command, e);
        } finally {
            command.reset();
        }
    }
}
