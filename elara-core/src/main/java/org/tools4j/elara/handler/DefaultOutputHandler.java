/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.output.Output.Ack;

import static java.util.Objects.requireNonNull;

public class DefaultOutputHandler implements OutputHandler {

    private final Output output;
    private final CommandLoopback commandLoopback;
    private final ExceptionHandler exceptionHandler;

    public DefaultOutputHandler(final Output output,
                                final CommandLoopback commandLoopback,
                                final ExceptionHandler exceptionHandler) {
        this.output = requireNonNull(output);
        this.commandLoopback = requireNonNull(commandLoopback);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public Ack publish(final Event event, final boolean replay, final int retry) {
        try {
            return output.publish(event, replay, retry, commandLoopback);
        } catch (final Throwable t) {
            exceptionHandler.handleEventOutputException(event, t);
            return Ack.COMMIT;//to retry handle exception and be explicit
        }
    }
}
