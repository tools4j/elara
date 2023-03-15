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
package org.tools4j.elara.format;

import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry.Type;

import java.util.concurrent.TimeUnit;

import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;
import static org.tools4j.elara.format.MessagePrinter.composite;

public interface MessagePrinters {

    static MessagePrinters defaults() {
        return new DefaultMessagePrinters();
    }

    static MessagePrinters defaults(final TimeUnit timeUnit, final long interval) {
        return defaults(TimeFormatter.formatterFor(timeUnit), interval);
    }

    static MessagePrinters defaults(final TimeFormatter timeFormatter, final long interval) {
        return new DefaultMessagePrinters(timeFormatter, interval);
    }

    default MessagePrinter<DataFrame> frame() {
        return composite(
                (line, entryId, frame) -> frame.type() == COMMAND_TYPE ? 0 : 1,
                command(),
                event()
        );
    }

    default MessagePrinter<MetricsStoreEntry> metrics() {
        return metrics(timeMetrics(), frequencyMetrics());
    }

    default MessagePrinter<MetricsStoreEntry> metricsWithLatencies() {
        return metrics(latencyMetrics(), frequencyMetrics());
    }

    default MessagePrinter<MetricsStoreEntry> metricsWithLatencyHistogram() {
        return metrics(latencyHistogram(), frequencyMetrics());
    }

    default MessagePrinter<MetricsStoreEntry> metrics(final MessagePrinter<MetricsStoreEntry> timeMetricsPrinter,
                                                      final MessagePrinter<MetricsStoreEntry> frequencyMetricsPrinter) {
        return composite(
                (line, entryId, emtry) -> emtry.type() == Type.TIME ? 0 : 1,
                timeMetricsPrinter,
                frequencyMetricsPrinter
        );
    }

    MessagePrinter<DataFrame> command();
    MessagePrinter<DataFrame> event();
    MessagePrinter<MetricsStoreEntry> frequencyMetrics();
    MessagePrinter<MetricsStoreEntry> timeMetrics();
    MessagePrinter<MetricsStoreEntry> latencyMetrics();
    MessagePrinter<MetricsStoreEntry> latencyHistogram();

}
