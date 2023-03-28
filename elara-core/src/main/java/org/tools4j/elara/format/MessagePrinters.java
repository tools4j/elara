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

import org.tools4j.elara.flyweight.CommandFrame;
import org.tools4j.elara.flyweight.DataFrame;
import org.tools4j.elara.flyweight.EventFrame;
import org.tools4j.elara.flyweight.FrameVisitor;
import org.tools4j.elara.flyweight.FrequencyMetricsFrame;
import org.tools4j.elara.flyweight.MetricsFrame;
import org.tools4j.elara.flyweight.TimeMetricsFrame;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public interface MessagePrinters {

    static MessagePrinters defaults() {
        return new PrettyMessagePrinters();
    }

    static MessagePrinters defaults(final TimeUnit timeUnit, final long interval) {
        return defaults(TimeFormatter.formatterFor(timeUnit), interval);
    }

    static MessagePrinters defaults(final TimeFormatter timeFormatter, final long interval) {
        return new PrettyMessagePrinters(timeFormatter, interval);
    }

    default MessagePrinter<DataFrame> frame() {
        return (line, entryId, frame, writer) -> frame.accept(new FrameVisitor() {
            final MessagePrinter<CommandFrame> commandPrinter = command();
            final MessagePrinter<EventFrame> eventPrinter = event();
            @Override
            public void commandFrame(final CommandFrame frame) {
                commandPrinter.print(line, entryId, frame, writer);
            }
            @Override
            public void eventFrame(final EventFrame frame) {
                eventPrinter.print(line, entryId, frame, writer);
            }
        });
    }

    default MessagePrinter<MetricsFrame> metrics() {
        return metrics(timeMetrics(), frequencyMetrics());
    }

    default MessagePrinter<MetricsFrame> metricsWithLatencies() {
        return metrics(latencyMetrics(), frequencyMetrics());
    }

    default MessagePrinter<MetricsFrame> metricsWithLatencyHistogram() {
        return metrics(latencyHistogram(), frequencyMetrics());
    }

    default MessagePrinter<MetricsFrame> metrics(final MessagePrinter<TimeMetricsFrame> timeMetricsPrinter,
                                                 final MessagePrinter<FrequencyMetricsFrame> frequencyMetricsPrinter) {
        requireNonNull(timeMetricsPrinter);
        requireNonNull(frequencyMetricsPrinter);
        return (line, entryId, frame, writer) -> frame.accept(new FrameVisitor() {
            @Override
            public void timeMetricsFrame(final TimeMetricsFrame frame) {
                timeMetricsPrinter.print(line, entryId, frame, writer);
            }
            @Override
            public void frequencyMetricsFrame(final FrequencyMetricsFrame frame) {
                frequencyMetricsPrinter.print(line, entryId, frame, writer);
            }
        });
    }

    MessagePrinter<CommandFrame> command();
    MessagePrinter<EventFrame> event();
    MessagePrinter<FrequencyMetricsFrame> frequencyMetrics();
    MessagePrinter<TimeMetricsFrame> timeMetrics();
    MessagePrinter<TimeMetricsFrame> latencyMetrics();
    MessagePrinter<TimeMetricsFrame> latencyHistogram();

}
