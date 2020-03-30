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
package org.tools4j.elara.log;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

public class MessageLogPrinter implements AutoCloseable {

    private final PrintWriter printWriter;
    private final boolean close;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public MessageLogPrinter() {
        this(System.out, false);
    }

    public MessageLogPrinter(final OutputStream outputStream) {
        this(outputStream, true);
    }

    public MessageLogPrinter(final OutputStream outputStream, final boolean close) {
        this(new OutputStreamWriter(outputStream), close);
    }

    public MessageLogPrinter(final Writer writer) {
        this(writer, true);
    }

    public MessageLogPrinter(final Writer writer, final boolean close) {
        this.printWriter = writer instanceof PrintWriter ? (PrintWriter)writer : new PrintWriter(writer);
        this.close = close;
    }

    public void flush() {
        printWriter.flush();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            if (close) {
                printWriter.close();
            } else {
                printWriter.flush();
            }
        }
    }

    public <M> void print(final MessageLog.Poller<M> poller) {
        final MessageFormatter<M> formatter = (line, msg, printWriter) ->
                printWriter.printf("[%s]: %s", line, msg);
        print(poller, formatter);
    }

    public <M> void print(final MessageLog.Poller<M> poller, final MessageFormatter<? super M> formatter) {
        print(poller, msg -> true, formatter);
    }

    public <M> void print(final MessageLog.Poller<M> poller,
                          final Predicate<? super M> filter,
                          final MessageFormatter<? super M> formatter) {
        final MessageLog.Handler<M> handler = (index, message) -> {
            if (filter.test(message)) {
                formatter.format(index, message, printWriter);
            }
        };
        while (poller.poll(handler) > 0);
        flush();
    }
}
