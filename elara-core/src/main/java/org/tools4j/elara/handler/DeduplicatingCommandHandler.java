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
package org.tools4j.elara.handler;

import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;

import static java.util.Objects.requireNonNull;

public class DeduplicatingCommandHandler implements CommandHandler {

    private final BaseState baseState;
    private final CommandHandler commandHandler;
    private final ExceptionHandler exceptionHandler;
    private final DuplicateHandler duplicateHandler;

    public DeduplicatingCommandHandler(final BaseState baseState,
                                       final CommandHandler commandHandler,
                                       final ExceptionHandler exceptionHandler,
                                       final DuplicateHandler duplicateHandler) {
        this.baseState = requireNonNull(baseState);
        this.commandHandler = requireNonNull(commandHandler);
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.duplicateHandler = requireNonNull(duplicateHandler);
    }

    @Override
    public void onCommand(final Command command) {
        if (eventAppliedForCommand(command)) {
            skipCommand(command);
        } else {
            processCommand(command);
        }
    }

    protected boolean eventAppliedForCommand(final Command command) {
        return baseState.eventAppliedForCommand(command.sourceId(), command.sourceSequence());
    }

    protected void processCommand(final Command command) {
        try {
            commandHandler.onCommand(command);
        } catch (final Throwable t) {
            exceptionHandler.handleCommandProcessorException(command, t);
        }
    }

    protected void skipCommand(final Command command) {
        try {
            duplicateHandler.skipCommandProcessing(command);
        } catch (final Throwable t) {
            exceptionHandler.handleCommandProcessorException(command, t);
        }
    }
}
