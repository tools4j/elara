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
package org.tools4j.elara.samples.hash;

import org.tools4j.elara.chronicle.ChronicleLogPrinter;
import org.tools4j.elara.format.DataFrameFormatter;
import org.tools4j.elara.format.DefaultMessagePrinters;
import org.tools4j.elara.format.HistogramFormatter;
import org.tools4j.elara.format.LatencyFormatter;
import org.tools4j.elara.format.MetricsFormatter;
import org.tools4j.elara.format.TimeFormatter;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry;

public class NanoChronicleLogPrinter {

    public static void main(final String... args) {
        ChronicleLogPrinter.run(new DefaultMessagePrinters() {
            @Override
            public DataFrameFormatter dataFrameFormatter() {
                return DataFrameFormatter.create(TimeFormatter.NANOS);
            }

            @Override
            public MetricsFormatter metricsFormatter() {
                return MetricsFormatter.create(TimeFormatter.NANOS);
            }

            @Override
            public LatencyFormatter latencyFormatter() {
                return LatencyFormatter.create(TimeFormatter.NANOS);
            }

            @Override
            public HistogramFormatter histogramFormatter() {
                return new HistogramFormatter() {
                    @Override
                    public long intervalValue(final long line, final long entryId, final MetricsLogEntry entry) {
                        return 100_000_000;
                    }

                    @Override
                    public TimeFormatter timeFormatter() {
                        return TimeFormatter.NANOS;
                    }
                };
            }
        }, args);
    }

}
