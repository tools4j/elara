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

import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.output.Output.Ack;

import static java.util.Objects.requireNonNull;

/**
 * Output handler that retries and gives up after a number of failures.  As a consequence the
 * {@link #publish(Event, boolean, int) publish(..)} method never returns {@link Ack#RETRY}.
 */
public class RetryOutputHandler implements OutputHandler {

    private final int nRetries;
    private final Output output;
    private final ExceptionHandler exceptionHandler;
    private final Exception outputRetriesFailed;

    public RetryOutputHandler(final int nRetries,
                              final Output output,
                              final ExceptionHandler exceptionHandler) {
        this.nRetries = nRetries;
        this.output = requireNonNull(output);
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.outputRetriesFailed = new IllegalStateException("Event output failed after " + nRetries + " retry attempts");
    }

    public static OutputHandler create(final int nRetries,
                                       final Output output,
                                       final ExceptionHandler exceptionHandler) {
        if (output == Output.NOOP) {
            return OutputHandler.NOOP;
        }
        return new RetryOutputHandler(nRetries, output, exceptionHandler);
    }

    @Override
    public Ack publish(final Event event, final boolean replay, final int initialRetry) {
        try {
            for (int retry = initialRetry; retry < nRetries; retry++) {
                final Ack ack = output.publish(event, replay, retry);
                if (Ack.RETRY != ack) {
                    return ack;
                }
            }
            exceptionHandler.handleEventOutputException(event, outputRetriesFailed);
        } catch (final Throwable t) {
            exceptionHandler.handleEventOutputException(event, t);
        }
        return Ack.COMMIT;
    }
}
