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
package org.tools4j.elara.format;

import org.tools4j.elara.plugin.metrics.LatencyMetric;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry;
import org.tools4j.elara.plugin.metrics.MetricsStoreEntry.Type;
import org.tools4j.elara.plugin.metrics.TimeMetric;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import static java.util.Objects.requireNonNull;

public interface LatencyFormatter extends MetricsFormatter {

    LatencyFormatter DEFAULT = new LatencyFormatter() {};

    static LatencyFormatter create(final TimeFormatter timeFormatter) {
        requireNonNull(timeFormatter);
        return new LatencyFormatter() {
            @Override
            public TimeFormatter timeFormatter() {
                return timeFormatter;
            }
        };
    }

    ThreadLocal<long[]> commandTimes = ThreadLocal.withInitial(() -> new long[TimeMetric.count()]);
    ThreadLocal<long[]> eventTimes = ThreadLocal.withInitial(() -> new long[TimeMetric.count()]);

    @Override
    default Object type(long line, long entryId, MetricsStoreEntry entry) {
        final Type type = entry.type();
        switch (entry.type()) {
            case TIME:
                return "LATE";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }

    @Override
    default Object metricsCount(long line, long entryId, MetricsStoreEntry entry) {
        if (entry.type() != Type.TIME) {
            return MetricsFormatter.super.metricsCount(line, entryId, entry);
        }
        return metricsSet(line, entryId, entry).size();
    }

    default EnumSet<LatencyMetric> metricsSet(long line, long entryId, MetricsStoreEntry entry) {
        final Predicate<LatencyMetric> skip;
        switch (entry.target()) {
            case COMMAND: skip = metric ->
                    Target.EVENT.both(metric.start(), metric.end()) || Target.OUTPUT.both(metric.start(), metric.end());
                break;
            case EVENT: skip = metric ->
                    Target.COMMAND.both(metric.start(), metric.end()) || Target.OUTPUT.both(metric.start(), metric.end());
                break;
            case OUTPUT: skip = metric ->
                    Target.COMMAND.both(metric.start(), metric.end()) || Target.EVENT.both(metric.start(), metric.end());
                break;
            default: skip = metric -> false;
        }
        final EnumSet<LatencyMetric> zeroSet = EnumSet.allOf(LatencyMetric.class);
        final EnumSet<LatencyMetric> oneSet = EnumSet.noneOf(LatencyMetric.class);
        final EnumSet<LatencyMetric> twoSet = EnumSet.noneOf(LatencyMetric.class);
        for (final TimeMetric timeMetric : TimeMetric.values()) {
            if (commandTimes.get()[timeMetric.ordinal()] != 0 || eventTimes.get()[timeMetric.ordinal()] != 0 ||
                    entry.target().contains(entry.flags(), timeMetric)) {
                for (final LatencyMetric latencyMetric : oneSet) {
                    if (latencyMetric.involves(timeMetric) && !skip.test(latencyMetric)) {
                        twoSet.add(latencyMetric);
                    }
                }
                for (final LatencyMetric latencyMetric : zeroSet) {
                    if (latencyMetric.involves(timeMetric) && !skip.test(latencyMetric)) {
                        oneSet.add(latencyMetric);
                    }
                }
            }
        }
        return twoSet;
    }

    default void cacheTimeValues(long line, long entryId, MetricsStoreEntry entry) {
        if (entry.type() != Type.TIME) {
            return;
        }
        final Consumer<long[]> cacher = times -> {
            Arrays.fill(times, 0);
            final int count = entry.count();
            for (int i = 0; i < count; i++) {
                final TimeMetric metric = entry.target().metric(entry.flags(), i);
                final long time = entry.time(i);
                times[metric.ordinal()] = time;
            }
        };
        switch (entry.target()) {
            case COMMAND:
                cacher.accept(commandTimes.get());
                break;
            case EVENT:
                cacher.accept(eventTimes.get());
                break;
            default:
                //nothing to cache
        }
    }
    @Override
    default Object metricsValues(long line, long entryId, MetricsStoreEntry entry) {
        if (entry.type() != Type.TIME) {
            return MetricsFormatter.super.metricsValues(line, entryId, entry);
        }
        cacheTimeValues(line, entryId, entry);
        final EnumSet<LatencyMetric> set = metricsSet(line, entryId, entry);
        final StringWriter sw = new StringWriter(set.size() * 16);
        final PrintWriter pw = new PrintWriter(sw);
        int index = 0;
        for (final LatencyMetric metric : set) {
            final MessagePrinter<MetricValue> valuePrinter = metricValuePrinter(line, entryId, entry, index);
            final MetricValue value = metricValue(line, entryId, entry, metric, index);
            valuePrinter.print(line, entryId, value, pw);
            index++;
        }
        pw.flush();
        return sw.toString();
    }

    @Override
    default MetricValue metricValue(final long line, final long entryId, final MetricsStoreEntry entry, final int index) {
        if (entry.type() != Type.TIME) {
            return MetricsFormatter.super.metricValue(line, entryId, entry, index);
        }
        final List<LatencyMetric> list = new ArrayList<>(metricsSet(line, entryId, entry));
        final LatencyMetric metric = index >= 0 && index < list.size() ? list.get(index) : null;
        return metricValue(line, entryId, entry, metric, index);
    }

    default MetricValue metricValue(final long line, final long entryId, final MetricsStoreEntry entry, final LatencyMetric metric, final int index) {
        final Target target = entry.target();
        final ToLongFunction<TimeMetric> timeLookup = timeMetric -> {
            final long commandTime = commandTimes.get()[timeMetric.ordinal()];
            if (commandTime != 0) {
                return commandTime;
            }
            final long eventTime = eventTimes.get()[timeMetric.ordinal()];
            if (eventTime != 0) {
                return eventTime;
            }
            final int count = entry.count();
            for (int i = 0; i < count; i++) {
                final TimeMetric m = target.metric(entry.flags(), i);
                if (m == timeMetric) {
                    return entry.time(i);
                }
            }
            return 0;
        };
        final long start = timeLookup.applyAsLong(metric.start());
        final long end = timeLookup.applyAsLong(metric.end());
        final long value = start != 0 && end != 0 ? end - start : 0;
        return new DefaultMetricValue(metric, value, index);
    }

}
