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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.tools4j.elara.flyweight.Flyweight;
import org.tools4j.elara.flyweight.FlyweightDataFrame;
import org.tools4j.elara.format.MessagePrinter;
import org.tools4j.elara.format.MessagePrinters;
import org.tools4j.elara.log.MessageLogPrinter;
import org.tools4j.elara.plugin.metrics.FlyweightMetricsLogEntry;
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.run.ThreadLike;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public class ChronicleLogPrinter implements AutoCloseable {

    private final MessageLogPrinter messageLogPrinter;

    public ChronicleLogPrinter() {
        this(System.out, false);
    }

    public ChronicleLogPrinter(final OutputStream outputStream) {
        this(outputStream, true);
    }

    public ChronicleLogPrinter(final OutputStream outputStream, final boolean close) {
        this(new OutputStreamWriter(outputStream), close);
    }

    public ChronicleLogPrinter(final Writer writer) {
        this(writer, true);
    }

    public ChronicleLogPrinter(final Writer writer, final boolean close) {
        this(new MessageLogPrinter(writer, close));
    }

    public ChronicleLogPrinter(final MessageLogPrinter messageLogPrinter) {
        this.messageLogPrinter = requireNonNull(messageLogPrinter);
    }

    public void flush() {
        messageLogPrinter.flush();
    }

    @Override
    public void close() {
        messageLogPrinter.close();
    }

    public void print(final ChronicleQueue queue, final Flyweight<?> flyweight) {
        messageLogPrinter.print(new ChronicleLogPoller(queue), flyweight);
    }

    public <M> void print(final ChronicleQueue queue,
                          final Flyweight<M> flyweight,
                          final MessagePrinter<? super M> printer) {
        messageLogPrinter.print(new ChronicleLogPoller(queue), flyweight, msg -> true, printer);
    }

    public <M> void print(final ChronicleQueue queue,
                          final Flyweight<M> flyweight,
                          final Predicate<? super M> filter,
                          final MessagePrinter<? super M> printer) {
        messageLogPrinter.print(new ChronicleLogPoller(queue), flyweight, filter, printer);
    }

    public <M> void print(final ChronicleQueue queue,
                          final Flyweight<M> flyweight,
                          final Predicate<? super M> filter,
                          final MessagePrinter<? super M> printer,
                          final LoopCondition loopCondition) {
        messageLogPrinter.print(new ChronicleLogPoller(queue), flyweight, filter, printer, loopCondition);
    }

    public <M> ThreadLike printInBackground(final ChronicleQueue queue,
                                            final Flyweight<M> flyweight,
                                            final Predicate<? super M> filter,
                                            final MessagePrinter<? super M> printer) {
        return messageLogPrinter.printInBackground(new ChronicleLogPoller(queue), flyweight, filter, printer);
    }

    public static void main(final String... args) {
        final boolean metrics = args.length == 2 && ("-m".equals(args[0]) || "--metrics".equals(args[0]));
        final boolean latencies = args.length == 2 && ("-l".equals(args[0]) || "--latencies".equals(args[0]));
        final boolean histograms = args.length == 2 && ("-h".equals(args[0]) || "--histograms".equals(args[0]));
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !metrics && !latencies && !histograms)) {
            System.err.println("usage: " + ChronicleLogPrinter.class.getSimpleName() + "[-m|--metrics|-l|--latencies|-h|--histograms] <file>");
            System.exit(1);
        }
        final String fileName = metrics || latencies || histograms ? args[1] : args[0];
        final ChronicleQueue queue = ChronicleQueue.singleBuilder()
                .path(fileName)
                .wireType(WireType.BINARY_LIGHT)
                .readOnly(true)
                .build();
        if (metrics) {
            new ChronicleLogPrinter().print(queue, new FlyweightMetricsLogEntry(), MessagePrinters.METRICS);
        } else if (latencies) {
            new ChronicleLogPrinter().print(queue, new FlyweightMetricsLogEntry(), MessagePrinters.LATENCIES);
        } else if (histograms) {
            new ChronicleLogPrinter().print(queue, new FlyweightMetricsLogEntry(), MessagePrinters.HISTOGRAMS);
        } else {
            new ChronicleLogPrinter().print(queue, new FlyweightDataFrame(), MessagePrinters.FRAME);
        }
    }
}
