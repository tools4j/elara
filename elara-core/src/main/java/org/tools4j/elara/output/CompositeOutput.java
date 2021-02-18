/**
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
package org.tools4j.elara.output;

import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.event.Event;

import static java.util.Objects.requireNonNull;

public class CompositeOutput implements Output {

    private final Output[] outputs;
    private final ExceptionHandler exceptionHandler;

    public CompositeOutput(final Output[] outputs, final ExceptionHandler exceptionHandler) {
        this.outputs = requireNonNull(outputs);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public Ack publish(final Event event, final boolean replay, final int retry, final CommandLoopback loopback) {
        Ack ack = Ack.IGNORED;
        for (final Output output : outputs) {
            try {
                final Ack cur = output.publish(event, replay, retry, loopback);
                if (cur == Ack.COMMIT && ack == Ack.IGNORED) {
                    ack = Ack.COMMIT;
                } else if (cur == Ack.RETRY) {
                    ack = Ack.RETRY;
                }
            } catch (final Throwable t) {
                exceptionHandler.handleEventOutputException(event, t);
                //no retry, handle exception and be explicit for retry
            }
        }
        return ack;
    }

}
