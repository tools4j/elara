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

import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.MessageSender.SendingContext;
import org.tools4j.elara.stream.SendingResult;

import static java.util.Objects.requireNonNull;

public class MessageSendingCommandHandler implements CommandHandler {

    private final MessageSender messageSender;
    private final ExceptionHandler exceptionHandler;

    public MessageSendingCommandHandler(final MessageSender messageSender,
                                        final ExceptionHandler exceptionHandler) {
        this.messageSender = requireNonNull(messageSender);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public void onCommand(final Command command) {
        final int sourceId = command.sourceId();
        final long sourceSeq = command.sourceSequence();
        try (final SendingContext context = messageSender.sendingMessage()) {
            final int length = FlyweightCommand.writeHeaderAndPayload(
                    (short)0,
                    sourceId,
                    sourceSeq,
                    command.commandTime(),
                    command.payloadType(),
                    command.payload(),
                    0,
                    command.payload().capacity(),
                    context.buffer(),
                    0
            );
            if (SendingResult.SENT != context.send(length)) {
                throw new IllegalStateException("Command sending failed for command " + sourceId + ":" + sourceSeq);
            }
        } catch (final Throwable t) {
            exceptionHandler.handleException("Unhandled exception when sending command", command, t);
        }
    }
}
