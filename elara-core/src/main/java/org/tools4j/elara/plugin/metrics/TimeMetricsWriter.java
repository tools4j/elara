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
package org.tools4j.elara.plugin.metrics;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.TimeMetric.METRIC_APPENDING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.Target.COMMAND;

public class TimeMetricsWriter {

    private final TimeSource timeSource;
    private final MetricsConfig configuration;
    private final MetricsState state;
    private final Appender appender;
    private final int[] metricTypesByTargetOrdinal;
    private final TimeMetric[][] metricsByTargetOrdinal;

    public TimeMetricsWriter(final TimeSource timeSource,
                             final MetricsConfig configuration,
                             final MetricsState state) {
        this.timeSource = requireNonNull(timeSource);
        this.configuration = requireNonNull(configuration);
        this.state = requireNonNull(state);
        if (configuration.timeMetrics().isEmpty()) {
            throw new IllegalArgumentException("Configuration contains no time metrics");
        }
        this.appender = requireNonNull(configuration.timeMetricsStore(), "configuration.timeMetricsStore()")
                .appender();
        metricTypesByTargetOrdinal = new int[Target.length()];
        metricsByTargetOrdinal = new TimeMetric[Target.length()][];
        for (int ordinal = 0; ordinal < Target.length(); ordinal++) {
            final Target target = Target.byOrdinal(ordinal);
            final int metricTypes = FlyweightTimeMetrics.metricTypes(target, configuration.timeMetrics());
            final byte flags = FlyweightTimeMetrics.metricTypesFlags(metricTypes);
            final int count = target.count(flags);
            metricTypesByTargetOrdinal[ordinal] = metricTypes;
            metricsByTargetOrdinal[ordinal] = new TimeMetric[count];
            for (int i = 0; i < count; i++) {
                metricsByTargetOrdinal[ordinal][i] = target.metric(flags, i);
            }
        }
    }

    public int writeMetrics(final Command command) {
        final int metricTypes = metricTypesByTargetOrdinal[COMMAND.ordinal()];
        final TimeMetric[] metrics = metricsByTargetOrdinal[COMMAND.ordinal()];
        if (metrics.length == 0) {
            return 0;
        }
        try (final AppendingContext context = appender.appending()) {
            final MutableDirectBuffer buffer = context.buffer();
            int length = FlyweightTimeMetrics.writeHeader(command.sourceId(), command.sourceSequence(),
                    metricTypes, timeSource.currentTime(), metrics.length, buffer, 0);
            length += writeTimeValues(metrics, buffer, 0);
            context.commit(length);
        }
        return 1;
    }

    public int writeMetrics(final Target target, final Event event) {
        return writeMetrics(target, event.sourceId(), event.sourceSequence(), (short)event.eventIndex(),
                event.eventSequence());
    }

    public int writeMetrics(final Target target,
                            final int sourceId,
                            final long sourceSequence,
                            final short eventIndex,
                            final long eventSequence) {
        if (target == COMMAND) {
            throw new IllegalArgumentException("Command target not applicable for events");
        }
        final int metricTypes = metricTypesByTargetOrdinal[target.ordinal()];
        final TimeMetric[] metrics = metricsByTargetOrdinal[target.ordinal()];
        if (metrics.length == 0) {
            return 0;
        }
        try (final AppendingContext context = appender.appending()) {
            final MutableDirectBuffer buffer = context.buffer();
            int length = FlyweightTimeMetrics.writeHeader(sourceId, sourceSequence, eventIndex, eventSequence,
                    metricTypes, timeSource.currentTime(),
                    metrics.length, buffer, 0);
            length += writeTimeValues(metrics, buffer, 0);
            context.commit(length);
        }
        return 1;
    }

    private int writeTimeValues(final TimeMetric[] metrics, final MutableDirectBuffer buffer, final int offset) {
        int length = 0;
        for (int i = 0; i < metrics.length; i++) {
            final TimeMetric metric = metrics[i];
            final long time = metric == METRIC_APPENDING_TIME ? timeSource.currentTime() : state.time(metric);
            length += FlyweightTimeMetrics.writeTimeValue(i, time, buffer, offset);
            state.clear(metric);
        }
        return length;
    }
}
