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
package org.tools4j.elara.handler;

import org.agrona.DirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.source.CommandSourceProvider;
import org.tools4j.elara.stream.SendingResult;

import static java.util.Objects.requireNonNull;

public class CommandSendingCommandHandler implements CommandHandler {

    private final CommandSourceProvider sourceContextProvider;
    private final ExceptionHandler exceptionHandler;

    public CommandSendingCommandHandler(final CommandSourceProvider sourceContextProvider,
                                        final ExceptionHandler exceptionHandler) {
        this.sourceContextProvider = requireNonNull(sourceContextProvider);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public void onCommand(final Command command) {
        handleCommand(command, sourceContextProvider, exceptionHandler);
    }

    public static void handleCommand(final Command command,
                                     final CommandSourceProvider sourceContextProvider,
                                     final ExceptionHandler exceptionHandler) {
        final CommandSender commandSender = sourceContextProvider
                .sourceById(command.sourceId())
                .commandSender();
        handleCommand(command, commandSender, exceptionHandler);
    }

    public static void handleCommand(final Command command,
                                     final CommandSender commandSender,
                                     final ExceptionHandler exceptionHandler) {
        try {
            final int sourceId = command.sourceId();
            final long sourceSeq = command.sourceSequence();
            final long nextSeq = commandSender.nextCommandSequence();
            final DirectBuffer payload = command.payload();
            if (nextSeq < sourceSeq) {
                commandSender.source().transientCommandState().sourceSequenceGenerator().nextSequence(sourceSeq);
            } else if (nextSeq > sourceSeq) {
                throw new IllegalArgumentException("Unexpected command sequence " + sourceId + ":" + sourceSeq +
                        ", expected at least " + nextSeq);
            }
            final SendingResult result = commandSender.sendCommand(command.payloadType(), payload, 0, payload.capacity());
            if (result != SendingResult.SENT) {
                throw new IllegalStateException("Command sending failed for " + sourceId + ":" + sourceSeq +
                        ", result=" + result);
            }
        } catch (final Exception e) {
            exceptionHandler.handleException("Exception when sending command message", command, e);
        }
    }
}