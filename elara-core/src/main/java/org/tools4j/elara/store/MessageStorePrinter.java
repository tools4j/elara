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
package org.tools4j.elara.store;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentInvoker;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.tools4j.elara.flyweight.Flyweight;
import org.tools4j.elara.format.MessagePrinter;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Handler.Result;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

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
        this.printWriter = writer instanceof PrintWriter ? (PrintWriter) writer : new PrintWriter(writer);
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
        final Agent agent = printAgent(poller, flyweight, filter, printer);
        final AgentInvoker invoker = new AgentInvoker(Throwable::printStackTrace, null, agent);
        invoker.start();
        int printedAny;
        do {
            printedAny = invoker.invoke();
        } while (printedAny > 0);
    }

    public <M> Agent printAgent(final MessageStore.Poller poller,
                                final Flyweight<M> flyweight,
                                final Predicate<? super M> filter,
                                final MessagePrinter<? super M> printer) {
        final long[] linePtr = {0};
        final Handler handler = message -> {
            final M msg = flyweight.init(message, 0);
            final long line = linePtr[0]++;
            if (filter.test(msg)) {
                printer.print(line, poller.entryId(), msg, printWriter);
                flush();
            }
            return Result.POLL;
        };
        return new Agent() {
            @Override
            public int doWork() {
                return poller.poll(handler);
            }

            @Override
            public String roleName() {
                return "log-printer";
            }
        };
    }

    public <M> AgentRunner agentRunner(final MessageStore.Poller poller,
                                       final Flyweight<M> flyweight,
                                       final Predicate<? super M> filter,
                                       final MessagePrinter<? super M> printer) {
        final Agent agent = printAgent(poller, flyweight, filter, printer);
        return new AgentRunner(new BackoffIdleStrategy(), Throwable::printStackTrace, null, agent);
    }

    public <M> Thread printInBackground(final MessageStore.Poller poller,
                                        final Flyweight<M> flyweight,
                                        final Predicate<? super M> filter,
                                        final MessagePrinter<? super M> printer) {
        final AgentRunner runner = agentRunner(poller, flyweight, filter, printer);
        return AgentRunner.startOnThread(runner, r -> new Thread(null, r, "log-printer"));
    }
}
