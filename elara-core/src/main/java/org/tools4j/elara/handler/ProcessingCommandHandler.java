/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.log.MessageLog.Handler;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.log.MessageLog.Handler.Result.PEEK;
import static org.tools4j.elara.log.MessageLog.Handler.Result.POLL;

public class ProcessingCommandHandler implements Handler, CommandHandler {

    private final BaseState baseState;
    private final DefaultEventRouter eventRouter;
    private final CommandProcessor commandProcessor;
    private final ExceptionHandler exceptionHandler;
    private final DuplicateHandler duplicateHandler;

    private final FlyweightCommand flyweightCommand = new FlyweightCommand();

    public ProcessingCommandHandler(final BaseState baseState,
                                    final DefaultEventRouter eventRouter,
                                    final CommandProcessor commandProcessor,
                                    final ExceptionHandler exceptionHandler,
                                    final DuplicateHandler duplicateHandler) {
        this.baseState = requireNonNull(baseState);
        this.eventRouter = requireNonNull(eventRouter);
        this.commandProcessor = requireNonNull(commandProcessor);
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.duplicateHandler = requireNonNull(duplicateHandler);
    }

    @Override
    public Result onMessage(final DirectBuffer message) {
        return onCommand(flyweightCommand.init(message, 0));
    }

    @Override
    public Result onCommand(final Command command) {
        if (baseState.allEventsAppliedFor(command.id())) {
            skipCommand(command);
            return POLL;
        }
        if (baseState.processCommands()) {
            return processCommand(command);
        }
        return PEEK;
    }

    private Result processCommand(final Command command) {
        eventRouter.start(command);
        try {
            commandProcessor.onCommand(command, eventRouter);
        } catch (final Throwable t) {
            exceptionHandler.handleCommandProcessorException(command, t);
        }
        if (eventRouter.complete()) {
            return POLL;
        }
        return PEEK;
    }

    private void skipCommand(final Command command) {
        try {
            duplicateHandler.skipCommandProcessing(command);
        } catch (final Throwable t) {
            exceptionHandler.handleCommandProcessorException(command, t);
        }
    }
}
