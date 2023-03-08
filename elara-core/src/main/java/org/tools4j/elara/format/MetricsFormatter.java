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

import org.tools4j.elara.format.IteratorMessagePrinter.Item;
import org.tools4j.elara.format.IteratorMessagePrinter.ItemFormatter;
import org.tools4j.elara.plugin.metrics.FrequencyMetric;
import org.tools4j.elara.plugin.metrics.Metric;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing metrics data produced by the
 * {@link org.tools4j.elara.plugin.metrics.MetricsPlugin MetricsPlugin}.
 */
public interface MetricsFormatter extends ValueFormatter<MetricsStoreEntry> {

    MetricsFormatter DEFAULT = new MetricsFormatter() {};

    static MetricsFormatter create(final TimeFormatter timeFormatter) {
        requireNonNull(timeFormatter);
        return new MetricsFormatter() {
            @Override
            public TimeFormatter timeFormatter() {
                return timeFormatter;
            }
        };
    }

    interface MetricValue {
        Metric metric();
        long value();
    }

    /** Placeholder in format string for metrics descriptor's version value */
    String VERSION = "{version}";
    /** Placeholder in format string for metrics flags value */
    String FLAGS = "{flags}";
    /** Placeholder in format string for metrics type value */
    String TYPE = "{type}";
    /** Placeholder in format string for target associated with time metrics */
    String TARGET = "{target}";
    /** Placeholder in format string for command source value available when printing time metrics */
    String SOURCE = "{source}";
    /** Placeholder in format string for command sequence value available when printing time metrics */
    String SEQUENCE = "{sequence}";
    /** Placeholder in format string for event index value available when printing time metrics */
    String INDEX = "{index}";
    /** Placeholder in format string for time value */
    String TIME = "{time}";
    /** Placeholder in format string for choice value available when printing frequency metrics */
    String CHOICE = "{choice}";
    /** Placeholder in format string for repetition value available when printing frequency metrics */
    String REPETITION = "{repetition}";
    /** Placeholder in format string for interval value available when printing frequency metrics */
    String INTERVAL = "{interval}";
    /** Placeholder in format string for time unit for latencies and durations */
    String TIME_UNIT = "{time-unit}";
    /** Placeholder in format string for the number of metrics */
    String METRICS_COUNT = "{metrics-count}";
    /** Placeholder in format string for the metrics names */

    default Object line(long line, long entryId, MetricsStoreEntry entry) {return line;}
    default Object entryId(long line, long entryId, MetricsStoreEntry entry) {return entryId;}
    default Object version(long line,long entryId, MetricsStoreEntry entry) {return entry.version();}
    default Object flags(long line,long entryId, MetricsStoreEntry entry) {return entry.flags();}
    default Object type(long line, long entryId, MetricsStoreEntry entry) {
        final Type type = entry.type();
        switch (entry.type()) {
            case TIME:
                return "TIME";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }
    default Object target(long line, long entryId, MetricsStoreEntry entry) {return entry.target();}
    default Object time(long line, long entryId, MetricsStoreEntry entry) {
        return timeFormatter().formatDateTime(entry.time());
    }
    //time metrics
    default Object source(long line, long entryId, MetricsStoreEntry entry) {return entry.sourceId();}
    default Object sequence(long line, long entryId, MetricsStoreEntry entry) {return entry.sourceSequence();}
    default Object index(long line, long entryId, MetricsStoreEntry entry) {return entry.index();}
    //frequency metrics
    default Object choice(long line, long entryId, MetricsStoreEntry entry) {return entry.choice();}
    default Object repetition(long line, long entryId, MetricsStoreEntry entry) {return entry.repetition();}
    default Object interval(long line, long entryId, MetricsStoreEntry entry) {
        return timeFormatter().formatDuration(entry.interval());
    }
    default Object timeUnit(long line, long entryId, MetricsStoreEntry entry) {
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

    default Object metricsCount(long line, long entryId, MetricsStoreEntry entry) {return entry.count();}
    @Override
    default Object value(final String placeholder, final long line, final long entryId, final MetricsStoreEntry entry) {
        switch (placeholder) {
            case LINE_SEPARATOR: return System.lineSeparator();
            case MESSAGE: return entry;
            case LINE: return line(line, entryId, entry);
            case ENTRY_ID: return entryId(line, entryId, entry);
            case VERSION: return version(entryId, entryId, entry);
            case FLAGS: return flags(entryId, entryId, entry);
            case TYPE: return type(entryId, entryId, entry);
            case TARGET: return target(entryId, entryId, entry);
            case TIME: return time(entryId, entryId, entry);
            case SOURCE: return source(entryId, entryId, entry);
            case SEQUENCE: return sequence(entryId, entryId, entry);
            case INDEX: return index(entryId, entryId, entry);
            case CHOICE: return choice(entryId, entryId, entry);
            case REPETITION: return repetition(entryId, entryId, entry);
            case INTERVAL: return interval(entryId, entryId, entry);
            case TIME_UNIT: return timeUnit(entryId, entryId, entry);
            case METRICS_COUNT: return metricsCount(entryId, entryId, entry);
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
        return MetricValueFormatter.DEFAULT;
    }

    default ItemFormatter<MetricValue> metricValueFormatter() {
        return MetricValueFormatter.DEFAULT;
    }

    default Iterable<MetricValue> metricValues(final long line, final long entryId, final MetricsStoreEntry entry) {
        final int count = entry.count();
        final List<MetricValue> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final MetricValue value = metricValue(line, entryId, entry, i);
            values.add(value);
        }
        return values;
    }

    default MetricValue metricValue(final long line, final long entryId, final MetricsStoreEntry entry, final int index) {
        final Type type = entry.type();
        final Metric metric = type == Type.TIME ? entry.target().metric(entry.flags(), index) :
                FrequencyMetric.metric(entry.choice(), index);
        final long value = type == Type.TIME ? entry.time(index) : entry.counter(index);
        return new DefaultMetricValue(metric, value);
    }

    default TimeFormatter timeFormatter() {
        return TimeFormatter.DEFAULT;
    }
}
