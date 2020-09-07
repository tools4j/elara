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
package org.tools4j.elara.samples.hash;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.tools4j.elara.chronicle.ChronicleLogPrinter;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.FlyweightDataFrame;
import org.tools4j.elara.format.DataFrameFormatter;
import org.tools4j.elara.format.MetricsFormatter;
import org.tools4j.elara.plugin.metrics.FlyweightMetricsLogEntry;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry;

import java.time.Instant;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.tools4j.elara.format.MessagePrinters.frame;
import static org.tools4j.elara.format.MessagePrinters.metrics;

public class NanoChronicleLogPrinter {

    public static void main(String[] args) {
        //System.out.println(System.getProperty("java.class.path"));
        final boolean metrics = args.length == 2 && ("-m".equals(args[0]) || "--metrics".equals(args[0]));
        if (args.length < 1 || args.length > 2 || (args.length == 2 && !metrics)) {
            System.err.println("usage: " + NanoChronicleLogPrinter.class.getSimpleName() + "[-m|--metrics] <file>");
            System.exit(1);
        }
        final String fileName = metrics ? args[1] : args[0];
        final ChronicleQueue queue = ChronicleQueue.singleBuilder()
                .path(fileName)
                .wireType(WireType.BINARY_LIGHT)
                .build();
        if (metrics) {
            new ChronicleLogPrinter().print(queue, new FlyweightMetricsLogEntry(), metrics(new MetricsFormatter() {
                @Override
                public Object time(final long line, final long entryId, final MetricsLogEntry entry) {
                    return instantOfEpochNanos(entry.time());
                }
            }));
        } else {
            new ChronicleLogPrinter().print(queue, new FlyweightDataFrame(), frame(new DataFrameFormatter() {
                @Override
                public Object time(final long line, final long entryId, final DataFrame frame) {
                    return instantOfEpochNanos(frame.header().time());
                }
            }));
        }
    }

    private static Instant instantOfEpochNanos(final long epochNanos) {
        final long s = NANOSECONDS.toSeconds(epochNanos);
        final long n = epochNanos - SECONDS.toNanos(s);
        return Instant.ofEpochSecond(s, n);
    }
}
