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

import org.tools4j.elara.flyweight.MetricsFrame;
import org.tools4j.elara.format.IteratorMessagePrinter.Item;
import org.tools4j.elara.format.IteratorMessagePrinter.ItemFormatter;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing metrics data produced by the
 * {@link org.tools4j.elara.plugin.metrics.MetricsPlugin MetricsPlugin}.
 */
public interface MetricsFormatter<M extends MetricsFrame> extends ValueFormatter<M> {

    interface MetricValue {
        Metric metric();
        long value();
    }

    /** Placeholder in format string for metrics descriptor's version value */
    String VERSION = "{version}";
    /** Placeholder in format string for metrics flags value */
    String METRIC_TYPE = "{metric-type}";
    /** Placeholder in format string for target associated with time metrics */
    String METRIC_TIME = "{metric-time}";
    /** Placeholder in format string for choice value available when printing frequency metrics */
    String TIME_UNIT = "{time-unit}";
    /** Placeholder in format string for the number of metrics values */
    String VALUE_COUNT = "{value-count}";

    default TimeFormatter timeFormatter() {
        return TimeFormatter.DEFAULT;
    }

    default Object line(long line, long entryId, M frame) {return line;}
    default Object entryId(long line, long entryId, M frame) {return entryId;}
    default Object version(long line,long entryId, M frame) {return frame.header().version();}
    default Object metricType(long line, long entryId, M frame) {
        final MetricType type = frame.metricType();
        switch (frame.metricType()) {
            case TIME:
                return "TIME";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }
    default Object metricTime(long line, long entryId, M frame) {
        return timeFormatter().formatDateTime(frame.metricTime());
    }

    default Object timeUnit(long line, long entryId, M frame) {
        final TimeUnit timeUnit = timeFormatter().timeUnit();
        if (timeUnit != null) {
            switch (timeUnit) {
                case NANOSECONDS: return "ns";
                case MICROSECONDS: return "us";
                case MILLISECONDS: return "ms";
                case SECONDS: return "s";
                case MINUTES: return "m";
                case HOURS: return "h";
                case DAYS: return "d";
            }
        }
        return "";
    }

    default int valueCount(M frame) {
        return frame.valueCount();
    }

    default Object valueCount(long line, long entryId, M frame) {
        return valueCount(frame);
    }

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final M frame) {
        switch (placeholder) {
            case LINE_SEPARATOR: return System.lineSeparator();
            case MESSAGE: return frame;
            case LINE: return line(line, entryId, frame);
            case ENTRY_ID: return entryId(line, entryId, frame);
            case VERSION: return version(entryId, entryId, frame);
            case METRIC_TYPE: return metricType(entryId, entryId, frame);
            case METRIC_TIME: return metricTime(entryId, entryId, frame);
            case TIME_UNIT: return timeUnit(entryId, entryId, frame);
            case VALUE_COUNT: return valueCount(entryId, entryId, frame);
            default: return placeholder;
        }
    }

    interface MetricValueFormatter extends ItemFormatter<MetricValue> {
        MetricValueFormatter DEFAULT = new MetricValueFormatter() {};

        /** Placeholder in metrics-values string for the name of a metric */
        String METRIC_NAME = "{metric-name}";
        /** Placeholder in metrics-values string for the value of a metric */
        String METRIC_VALUE = "{metric-value}";

        default Object metricName(final long line, final long entryId, final MetricValue metricValue) {
            return metricValue.metric().displayName();
        }
        default Object metricValue(final long line, final long entryId, final MetricValue metricValue) {
            return metricValue.value();
        }

        @Override
        default Object value(final String placeholder, final long line, final long entryId, final Item<? extends MetricValue> metricItem) {
            switch (placeholder) {
                case METRIC_NAME: return metricName(line, entryId, metricItem.itemValue());
                case METRIC_VALUE: return metricValue(line, entryId, metricItem.itemValue());
                default: return ItemFormatter.super.value(placeholder, line, entryId, metricItem);
            }
        }

        default MetricValueFormatter withTimeFormatter(final TimeFormatter timeFormatter) {
            requireNonNull(timeFormatter);
            return new MetricValueFormatter() {
                @Override
                public Object value(final String placeholder, final long line, final long entryId, final Item<? extends MetricValue> item) {
                    if (METRIC_VALUE.equals(placeholder)) {
                        final MetricValue metricValue = item.itemValue();
                        switch (metricValue.metric().type()) {
                            case TIME: return timeFormatter.formatTime(metricValue.value());
                            case LATENCY: return timeFormatter.formatDuration(metricValue.value());
                        }
                    }
                    return MetricValueFormatter.this.value(placeholder, line, entryId, item);
                }
            };
        }
    }

    default ItemFormatter<MetricValue> metricNameFormatter() {
        return MetricValueFormatter.DEFAULT.withTimeFormatter(timeFormatter());
    }

    default ItemFormatter<MetricValue> metricValueFormatter() {
        return MetricValueFormatter.DEFAULT.withTimeFormatter(timeFormatter());
    }

    default Iterable<MetricValue> metricValues(final long line, final long entryId, final M frame) {
        final int count = valueCount(frame);
        final List<MetricValue> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final MetricValue value = metricValue(line, entryId, frame, i);
            values.add(value);
        }
        return values;
    }

    MetricValue metricValue(long line, long entryId, M frame, int valueIndex);
}
