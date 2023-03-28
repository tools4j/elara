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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.tools4j.elara.flyweight.Flyweight;
import org.tools4j.elara.flyweight.FlyweightDataFrame;
import org.tools4j.elara.flyweight.FlyweightMetricsFrame;
import org.tools4j.elara.format.CsvMessagePrinters;
import org.tools4j.elara.format.MessagePrinter;
import org.tools4j.elara.format.MessagePrinters;
import org.tools4j.elara.format.TimeFormatter;
import org.tools4j.elara.store.MessageStorePrinter;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ChronicleMessageStorePrinter implements AutoCloseable {

    private final MessageStorePrinter messageStorePrinter;

    public ChronicleMessageStorePrinter() {
        this(System.out, false);
    }

    public ChronicleMessageStorePrinter(final OutputStream outputStream) {
        this(outputStream, true);
    }

    public ChronicleMessageStorePrinter(final OutputStream outputStream, final boolean close) {
        this(new OutputStreamWriter(outputStream), close);
    }

    public ChronicleMessageStorePrinter(final Writer writer) {
        this(writer, true);
    }

    public ChronicleMessageStorePrinter(final Writer writer, final boolean close) {
        this(new MessageStorePrinter(writer, close));
    }

    public ChronicleMessageStorePrinter(final MessageStorePrinter messageStorePrinter) {
        this.messageStorePrinter = requireNonNull(messageStorePrinter);
    }

    public void flush() {
        messageStorePrinter.flush();
    }

    @Override
    public void close() {
        messageStorePrinter.close();
    }

    public void print(final ChronicleQueue queue, final Flyweight<?> flyweight) {
        messageStorePrinter.print(new ChroniclePoller(queue), flyweight);
    }

    public <M> void print(final ChronicleQueue queue,
                          final Flyweight<M> flyweight,
                          final MessagePrinter<? super M> printer) {
        messageStorePrinter.print(new ChroniclePoller(queue), flyweight, msg -> true, printer);
    }

    public <M> void print(final ChronicleQueue queue,
                          final Flyweight<M> flyweight,
                          final Predicate<? super M> filter,
                          final MessagePrinter<? super M> printer) {
        messageStorePrinter.print(new ChroniclePoller(queue), flyweight, filter, printer);
    }

    public <M> Agent printAgent(final ChronicleQueue queue, final Flyweight<M> flyweight, final Predicate<? super M> filter, final MessagePrinter<? super M> printer) {
        return messageStorePrinter.printAgent(new ChroniclePoller(queue), flyweight, filter, printer);
    }

    public <M> AgentRunner agentRunner(final ChronicleQueue queue, final Flyweight<M> flyweight, final Predicate<? super M> filter, final MessagePrinter<? super M> printer) {
        return messageStorePrinter.agentRunner(new ChroniclePoller(queue), flyweight, filter, printer);
    }

    public <M> Thread printInBackground(final ChronicleQueue queue, final Flyweight<M> flyweight, final Predicate<? super M> filter, final MessagePrinter<? super M> printer) {
        return messageStorePrinter.printInBackground(new ChroniclePoller(queue), flyweight, filter, printer);
    }

    private static TimeUnit timeUnit(final int index, final String... args) {
        if (index < args.length) {
            switch (args[index]) {
                case "ms":
                    return MILLISECONDS;
                case "us":
                    return TimeUnit.MICROSECONDS;
                case "ns":
                    return TimeUnit.NANOSECONDS;
            }
        }
        return null;
    }

    private static Class<? extends MessagePrinters> format(final int index, final String... args) {
        try {
            switch (args[index]) {
                case "default": return MessagePrinters.class;
                case "csv": return CsvMessagePrinters.class;
                default: return Class.forName(args[index]).asSubclass(MessagePrinters.class);
            }
        } catch (final Exception e) {
            return null;
        }
    }

    private static MessagePrinters messagePrinters(final Class<? extends MessagePrinters> type) {
        try {
            if (type != null) {
                return type.newInstance();
            }
        } catch (final Exception e) {
            System.err.println("WARN: could not instantiate formatter " + type.getName() + " [error=" + e + "], using default");
        }
        return MessagePrinters.defaults();
    }

    private static MessagePrinters messagePrinters(final Class<? extends MessagePrinters> type,
                                                   final TimeUnit unit,
                                                   final long interval) {
        try {
            if (type != null) {
                final TimeFormatter formatter = TimeFormatter.formatterFor(unit);
                return type.getConstructor(TimeFormatter.class, long.class).newInstance(formatter, interval);
            }
        } catch (final Exception e) {
            System.err.println("WARN: could not instantiate formatter " + type.getName() + " [error=" + e + "], using default");
        }
        return MessagePrinters.defaults(unit, interval);
    }

    private static long interval(final int index, final String... args) {
        try {
            return Long.parseLong(args[index]);
        } catch (final Exception e) {
            return -1;
        }
    }

    private static final int ERR_SYNTAX = 1;
    private static final int ERR_FORMAT = 2;
    private static final int ERR_TIME_UNIT = 3;
    private static final int ERR_INTERVAL = 4;
    private static final int ERR_EXTRA_ARGS = 5;

    public static void main(final String... args) {
        final int n = args.length;
        Class<? extends MessagePrinters> format = null;
        TimeUnit timeUnit = null;
        long interval = -1;
        boolean metrics = false;
        boolean latencies = false;
        boolean histograms = false;
        String file = null;
        int index = 0;
        do {
            if (index >= n) {
                printHelp();
                System.exit(ERR_SYNTAX);
            }
            final String arg = args[index++];
            switch (arg) {
                case "-f":
                case "--format":
                    if (format != null || (format = format(index++, args)) == null) {
                        printHelp();
                        System.exit(ERR_FORMAT);
                    }
                    break;
                case "-t":
                case "--time":
                    if (timeUnit != null || (timeUnit = timeUnit(index++, args)) == null) {
                        printHelp();
                        System.exit(ERR_TIME_UNIT);
                    }
                    break;
                case "-i":
                case "-interval":
                    if (interval != -1 || (interval = interval(index++, args)) == -1) {
                        printHelp();
                        System.exit(ERR_INTERVAL);
                    }
                    break;
                case "-m":
                case "--metrics":
                    metrics = true;
                    break;
                case "-l":
                case "--latencies":
                    latencies = true;
                    break;
                case "-h":
                case "--histograms":
                    histograms = true;
                    break;
                default:
                    if (index < n) {
                        printHelp();
                        System.exit(ERR_EXTRA_ARGS);
                    }
                    file = arg;
                    break;
            }
        } while (file == null);
        if (metrics && latencies || metrics && histograms || latencies && histograms) {
            printHelp();
            System.exit(3);
        }
        final ChronicleQueue queue = ChronicleQueue.singleBuilder()
                .path(file)
                .wireType(WireType.BINARY_LIGHT)
                .readOnly(true)
                .build();
        try (final ChronicleMessageStorePrinter storePrinter = new ChronicleMessageStorePrinter()) {
            final MessagePrinters msgPrinters = timeUnit == null ? messagePrinters(format) :
                    messagePrinters(format, timeUnit, timeUnit.convert(interval >= 0 ? interval : 1000, MILLISECONDS));
            if (metrics) {
                storePrinter.print(queue, new FlyweightMetricsFrame(), msgPrinters.metrics());
            } else if (latencies) {
                storePrinter.print(queue, new FlyweightMetricsFrame(), msgPrinters.metricsWithLatencies());
            } else if (histograms) {
                storePrinter.print(queue, new FlyweightMetricsFrame(), msgPrinters.metricsWithLatencyHistogram());
            } else {
                storePrinter.print(queue, new FlyweightDataFrame(), msgPrinters.frame());
            }
        }
    }

    private static void printHelp() {
        System.err.println("usage: " + ChronicleMessageStorePrinter.class.getSimpleName() + "[-f|--format <format>] [-t|--time <unit>] [-i|--interval <duration>] [-m|--metrics|-l|--latencies|-h|--histograms] <file>");
        System.err.println("    -f|--format <format>       <format> defines the output format");
        System.err.println("                               Valid <format> values are 'default', 'csv' or a class name of a MessagePrinters implementation");
        System.err.println("    -t|--time <unit>           Defines the unit of the time source that was used when running Elara.");
        System.err.println("                               Valid <unit> values are 'ms', 'us', 'ns' for milliseconds, microseconds and nanoseconds, respectively");
        System.err.println("    -i|--interval <duration>   Specifies the interval in milliseconds after which a histogram is printed and reset.");
        System.err.println("                               Default value is 1000 (1s). Valid only in combination with the histogram option (-h) and ignored otherwise.");
        System.err.println("    -m|--metrics\n" +
                "}\n               <file> refers to a time and/or frequency metric file");
        System.err.println("    -l|--latencies             <file> refers to a time and/or frequency metric file, with times printed as latencies");
        System.err.println("    -h|--histograms            <file> refers to a time and/or frequency metric file, with times summarised as latency histograms");
        System.exit(1);
    }
}
