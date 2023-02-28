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

import org.tools4j.elara.command.SourceIds;
import org.tools4j.elara.flyweight.TimeMetricsFrame;

import static java.util.Objects.requireNonNull;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing time metrics data produced by the
 * {@link org.tools4j.elara.plugin.metrics.MetricsPlugin MetricsPlugin}.
 */
public interface TimeMetricsFormatter extends MetricsFormatter<TimeMetricsFrame> {

    TimeMetricsFormatter DEFAULT = new TimeMetricsFormatter() {};

    static TimeMetricsFormatter create(final TimeFormatter timeFormatter) {
        requireNonNull(timeFormatter);
        return new TimeMetricsFormatter() {
            @Override
            public TimeFormatter timeFormatter() {
                return timeFormatter;
            }
        };
    }

    /** Placeholder in format string for target value */
    String TARGET = "{target}";
    /** Placeholder in format string for command source-ID value */
    String SOURCE_ID = "{source-id}";
    /** Placeholder in format string for command sequence value */
    String SOURCE_SEQUENCE = "{source-sequence}";
    /** Placeholder in format string for event sequence value */
    String EVENT_SEQUENCE = "{event-sequence}";
    /** Placeholder in format string for event index value */
    String EVENT_INDEX = "{event-index}";

    default Object target(long line, long entryId, TimeMetricsFrame frame) {
        return frame.target();
    }
    default Object metricTime(long line, long entryId, TimeMetricsFrame frame) {
        return timeFormatter().formatDateTime(frame.metricTime());
    }
    default Object sourceId(long line, long entryId, TimeMetricsFrame frame) {
        return SourceIds.toString(frame.sourceId());
    }
    default Object sourceSequence(long line, long entryId, TimeMetricsFrame frame) {
        return frame.sourceSequence();
    }
    default Object eventSequence(long line, long entryId, TimeMetricsFrame frame) {
        return frame.eventSequence();
    }
    default Object eventIndex(long line, long entryId, TimeMetricsFrame frame) {
        return frame.eventIndex();
    }

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final TimeMetricsFrame frame) {
        switch (placeholder) {
            case TARGET: return target(entryId, entryId, frame);
            case SOURCE_ID: return sourceId(entryId, entryId, frame);
            case SOURCE_SEQUENCE: return sourceSequence(entryId, entryId, frame);
            case EVENT_SEQUENCE: return eventSequence(entryId, entryId, frame);
            case EVENT_INDEX: return eventIndex(entryId, entryId, frame);
            default: return MetricsFormatter.super.value(placeholder, line, entryId, frame);
        }
    }

    @Override
    default MetricValue metricValue(long line, long entryId, TimeMetricsFrame frame, int valueIndex) {
        return new DefaultMetricValue(frame.metric(valueIndex), frame.timeValue(valueIndex));
    }
}
