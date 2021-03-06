/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.log.MessageLog.AppendingContext;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.FlyweightMetricsLogEntry.writeTime;
import static org.tools4j.elara.plugin.metrics.FlyweightMetricsLogEntry.writeTimeMetricsHeader;
import static org.tools4j.elara.plugin.metrics.TimeMetric.METRIC_APPENDING_TIME;

public class TimeMetricsLogger {

    private final TimeSource timeSource;
    private final Configuration configuration;
    private final MetricsState state;
    private final Appender appender;

    public TimeMetricsLogger(final TimeSource timeSource,
                             final Configuration configuration,
                             final MetricsState state) {
        this.timeSource = requireNonNull(timeSource);
        this.configuration = requireNonNull(configuration);
        this.state = requireNonNull(state);
        if (configuration.timeMetrics().isEmpty()) {
            throw new IllegalArgumentException("Configuration contains no time metrics");
        }
        this.appender = requireNonNull(configuration.timeMetricsLog(), "configuration.timeMetricsLog()")
                .appender();
    }

    public int logMetrics(final Command command) {
        return logMetrics(Target.COMMAND, command.id(), FlyweightCommand.INDEX);
    }

    public int logMetrics(final Target target, final Event event) {
        if (target == Target.COMMAND) {
            throw new IllegalArgumentException("Command target not applicable for events");
        }
        final Event.Id id = event.id();
        return logMetrics(target, id.commandId(), (short)id.index());
    }

    private int logMetrics(final Target target, final Command.Id commandId, final short index) {
        final byte flags = target.flags(configuration.timeMetrics());
        final int count = target.count(flags);
        if (count == 0) {
            return 0;
        }
        final int source = commandId.source();
        final long sequence = commandId.sequence();
        try (final AppendingContext context = appender.appending()) {
            final MutableDirectBuffer buffer = context.buffer();
            final int headerLen = writeTimeMetricsHeader(flags, index, source, sequence, timeSource.currentTime(), buffer, 0);
            int offset = headerLen;
            for (int i = 0; i < count; i++) {
                final TimeMetric metric = target.metric(flags, i);
                final long time = metric == METRIC_APPENDING_TIME ? timeSource.currentTime() : state.time(metric);
                offset += writeTime(time, buffer, offset);
                state.clear(metric);
            }
            final int length = offset;
            context.commit(length);
        }
        return count;
    }
}
