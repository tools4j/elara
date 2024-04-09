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
package org.tools4j.elara.exception;

import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.logging.DefaultElaraLogger;
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger;

import static java.util.Objects.requireNonNull;

public class ExceptionLogger implements ExceptionHandler, DuplicateHandler{

    public static final ExceptionLogger DEFAULT = new ExceptionLogger(true);
    public static final ExceptionLogger DEFAULT_WITHOUT_STACK_TRACE = new ExceptionLogger(false);

    private final ElaraLogger logger;
    private final boolean logStackTrace;

    public ExceptionLogger(final boolean logStackTrace) {
        this(Logger.systemLogger(), logStackTrace);
    }

    public ExceptionLogger(final Logger.Factory loggerFactory, final boolean logStackTrace) {
        this(loggerFactory.create(ExceptionLogger.class), logStackTrace);
    }

    public ExceptionLogger(final Logger logger, final boolean logStackTrace) {
        this(DefaultElaraLogger.create(logger), logStackTrace);
    }

    public ExceptionLogger(final ElaraLogger logger, final boolean logStackTrace) {
        this.logger = requireNonNull(logger);
        this.logStackTrace = logStackTrace;
    }

    @Override
    public void skipCommandProcessing(final Command command) {
        logger.info("Skipping command: {}").replace(command).format();
    }

    @Override
    public void skipEventApplying(final Event event) {
        logger.debug("Skipping event: {}").replace(event).format();
    }

    @Override
    public void handleCommandProcessorException(final Command command, final Throwable t) {
        handleException("Unhandled exception when processing command: {}", command, t);
    }

    @Override
    public void handleEventApplierException(final Event event, final Throwable t) {
        handleException("Unhandled exception when applying event: {}", event, t);
    }

    @Override
    public void handleEventOutputException(final Event event, final Throwable t) {
        handleException("Unhandled exception when publishing event: {}", event, t);
    }

    @Override
    public void onError(final Throwable t) {
        logger.error("Unhandled exception: {}").replace(t).format();
        if (logStackTrace) {
            logger.logStackTrace(t);
        }
    }

    @Override
    public void handleException(final String message, final Object context, final Throwable t) {
        if (context != null) {
            logger.error(message).replace(context).replace(t).format();
        } else {
            logger.error(message).replace(t).format();
        }
        if (logStackTrace) {
            logger.logStackTrace(t);
        }
    }

}
