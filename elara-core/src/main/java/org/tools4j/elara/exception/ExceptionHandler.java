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
package org.tools4j.elara.exception;

import org.agrona.ErrorHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;

@FunctionalInterface
public interface ExceptionHandler extends ErrorHandler {

    void handleException(String message, Object context, Throwable t);

    default void handleException(final String message, final Throwable t) {
        handleException(message, null, t);
    }

    default void handleCommandProcessorException(final Command command, final Throwable t) {
        handleException("Unhandled exception when processing command", command, t);
    }

    default void handleEventApplierException(final Event event, final Throwable t) {
        handleException("Unhandled exception when applying event", event, t);
    }

    default void handleEventProcessorException(final Event event, final Throwable t) {
        handleException("Unhandled exception when processing event", event, t);
    }

    default void handleEventOutputException(final Event event, final Throwable t) {
        handleException("Unhandled exception when publishing event", event, t);
    }

    @Override
    default void onError(final Throwable t) {
        handleException("Unhandled exception", null, t);
    }

    static ExceptionHandler systemDefault() {
        return ExceptionLogger.DEFAULT;
    }

    static ExceptionHandler systemDefaultWithoutStackTrace() {
        return ExceptionLogger.DEFAULT_WITHOUT_STACK_TRACE;
    }
}
