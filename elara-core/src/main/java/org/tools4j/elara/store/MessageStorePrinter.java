/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.store;

import org.tools4j.elara.flyweight.Flyweight;
import org.tools4j.elara.format.MessagePrinter;
import org.tools4j.elara.stream.MessageStream.Handler.Result;
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.run.StoppableThread;
import org.tools4j.nobark.run.ThreadLike;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class MessageStorePrinter implements AutoCloseable {

    private final PrintWriter printWriter;
    private final boolean close;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public MessageStorePrinter() {
        this(System.out, false);
    }

    public MessageStorePrinter(final OutputStream outputStream) {
        this(outputStream, true);
    }

    public MessageStorePrinter(final OutputStream outputStream, final boolean close) {
        this(new OutputStreamWriter(outputStream), close);
    }

    public MessageStorePrinter(final Writer writer) {
        this(writer, true);
    }

    public MessageStorePrinter(final Writer writer, final boolean close) {
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

    public void print(final MessageStore.Poller poller, final Flyweight<?> flyweight) {
        print(poller, flyweight, MessagePrinter.DEFAULT);
    }

    public <M> void print(final MessageStore.Poller poller,
                          final Flyweight<M> flyweight,
                          final MessagePrinter<? super M> printer) {
        print(poller, flyweight, msg -> true, printer);
    }

    public <M> void print(final MessageStore.Poller poller,
                          final Flyweight<M> flyweight,
                          final Predicate<? super M> filter,
                          final MessagePrinter<? super M> printer) {
        print(poller, flyweight, filter, printer, workDone -> workDone);
    }

    public <M> void print(final MessageStore.Poller poller,
                          final Flyweight<M> flyweight,
                          final Predicate<? super M> filter,
                          final MessagePrinter<? super M> printer,
                          final LoopCondition loopCondition) {
        final long[] linePtr = {0};
        final MessageStore.Handler handler = message -> {
            final M msg = flyweight.init(message, 0);
            final long line = linePtr[0]++;
            if (filter.test(msg)) {
                printer.print(line, poller.entryId(), msg, printWriter);
                flush();
            }
            return Result.POLL;
        };
        boolean workDone;
        do {
            workDone = poller.poll(handler) > 0;
        } while (loopCondition.loopAgain(workDone));
    }

    public <M> ThreadLike printInBackground(final MessageStore.Poller poller,
                                            final Flyweight<M> flyweight,
                                            final Predicate<? super M> filter,
                                            final MessagePrinter<? super M> printer) {
        requireNonNull(poller);
        requireNonNull(flyweight);
        requireNonNull(filter);
        requireNonNull(printer);
        return StoppableThread.start(runningCondition -> () ->
                print(poller, flyweight, filter, printer, workDone -> runningCondition.keepRunning()),
                r -> new Thread(null, r, "store-printer")
        );
    }

}
