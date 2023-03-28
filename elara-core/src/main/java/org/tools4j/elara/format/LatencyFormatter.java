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

import org.tools4j.elara.flyweight.TimeMetricsFrame;
import org.tools4j.elara.plugin.metrics.LatencyMetric;
import org.tools4j.elara.plugin.metrics.MetricType;
import org.tools4j.elara.plugin.metrics.TimeMetric;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public interface LatencyFormatter extends TimeMetricsFormatter {

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

    ThreadLocal<long[]> commandTimes = ThreadLocal.withInitial(() -> new long[TimeMetric.length()]);
    ThreadLocal<long[]> eventTimes = ThreadLocal.withInitial(() -> new long[TimeMetric.length()]);

    @Override
    default Object metricType(long line, long entryId, TimeMetricsFrame frame) {
        final MetricType type = frame.metricType();
        switch (frame.metricType()) {
            case TIME:
                return "LATE";
            case FREQUENCY:
                return "FREQ";
        }
        return type;
    }

    @Override
    default int valueCount(TimeMetricsFrame frame) {
        return metricsSet(frame).size();
    }

    default EnumSet<LatencyMetric> metricsSet(TimeMetricsFrame frame) {
        final Target target = frame.target();
        final Predicate<LatencyMetric> skip;
        switch (target) {
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
                    frame.hasMetric(timeMetric)) {
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

    default void cacheTimeValues(long line, long entryId, TimeMetricsFrame frame) {
        final Consumer<long[]> cacher = times -> {
            Arrays.fill(times, 0);
            final int count = frame.valueCount();
            for (int i = 0; i < count; i++) {
                final TimeMetric metric = frame.timeMetric(i);
                final long time = frame.timeValue(i);
                times[metric.ordinal()] = time;
            }
        };
        switch (frame.target()) {
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
    default Iterable<MetricValue> metricValues(final long line, final long entryId, final TimeMetricsFrame frame) {
        cacheTimeValues(line, entryId, frame);
        return metricsSet(frame)
                .stream()
                .map(latencyMetric -> metricValue(line, entryId, frame, latencyMetric))
                .collect(Collectors.toList());
    }

    @Override
    default MetricValue metricValue(final long line, final long entryId, final TimeMetricsFrame frame, final int index) {
        final List<LatencyMetric> list = new ArrayList<>(metricsSet(frame));
        final LatencyMetric metric = index >= 0 && index < list.size() ? list.get(index) : null;
        return metricValue(line, entryId, frame, metric);
    }

    default MetricValue metricValue(final long line, final long entryId, final TimeMetricsFrame frame, final LatencyMetric metric) {
        final ToLongFunction<TimeMetric> timeLookup = timeMetric -> {
            final long commandTime = commandTimes.get()[timeMetric.ordinal()];
            if (commandTime != 0) {
                return commandTime;
            }
            final long eventTime = eventTimes.get()[timeMetric.ordinal()];
            if (eventTime != 0) {
                return eventTime;
            }
            final int count = frame.valueCount();
            for (int i = 0; i < count; i++) {
                final TimeMetric m = frame.timeMetric(i);
                if (m == timeMetric) {
                    return frame.timeValue(i);
                }
            }
            return 0;
        };
        final long start = timeLookup.applyAsLong(metric.start());
        final long end = timeLookup.applyAsLong(metric.end());
        final long value = start != 0 && end != 0 ? end - start : 0;
        return new DefaultMetricValue(metric, value);
    }
}
