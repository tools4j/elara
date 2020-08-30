/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.plugin.metrics.FrequencyMetric;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry;
import org.tools4j.elara.plugin.metrics.MetricsLogEntry.Type;
import org.tools4j.elara.plugin.metrics.TimeMetric;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;

import static org.tools4j.elara.format.MessagePrinters.METRICS_VALUE_FORMAT_0;
import static org.tools4j.elara.format.MessagePrinters.METRICS_VALUE_FORMAT_N;

/**
 * Formats value for {@link MessagePrinter} when printing lines containing metrics data produced by the
 * {@link org.tools4j.elara.plugin.metrics.MetricsPlugin MetricsPlugin}.
 */
public interface MetricsFormatter extends ValueFormatter<MetricsLogEntry> {

    MetricsFormatter DEFAULT = new MetricsFormatter() {};

    interface MetricValue {
        Enum<?> name();
        long value();
        int index();
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
    /** Placeholder in format string for the number of metrics */
    String METRICS_COUNT = "{metrics-count}";
    /** Placeholder in format string for the metrics values */
    String METRICS_VALUES = "{metrics-values}";

    default Object line(long line, long entryId, MetricsLogEntry entry) {return line;}
    default Object entryId(long line, long entryId, MetricsLogEntry entry) {return entryId;}
    default Object version(long line,long entryId, MetricsLogEntry entry) {return entry.version();}
    default Object flags(long line,long entryId, MetricsLogEntry entry) {return entry.flags();}
    default Object type(long line, long entryId, MetricsLogEntry entry) {
        final Type type = entry.type();
        switch (entry.type()) {
            case TIME:
                return "T";
            case FREQUENCY:
                return "F";
        }
        return type;
    }
    default Object target(long line, long entryId, MetricsLogEntry entry) {return entry.target();}
    default Object time(long line, long entryId, MetricsLogEntry entry) {return Instant.ofEpochMilli(entry.time());}
    //time metrics
    default Object source(long line, long entryId, MetricsLogEntry entry) {return entry.source();}
    default Object sequence(long line, long entryId, MetricsLogEntry entry) {return entry.sequence();}
    default Object index(long line, long entryId, MetricsLogEntry entry) {return entry.index();}
    //frequency metrics
    default Object choice(long line, long entryId, MetricsLogEntry entry) {return entry.choice();}
    default Object repetition(long line, long entryId, MetricsLogEntry entry) {return entry.repetition();}
    default Object interval(long line, long entryId, MetricsLogEntry entry) {return entry.interval();}

    default Object metricsCount(long line, long entryId, MetricsLogEntry entry) {return entry.count();}
    default Object metricsValues(long line, long entryId, MetricsLogEntry entry) {
        final int count = entry.count();
        final StringWriter sw = new StringWriter(count * 16);
        final PrintWriter pw = new PrintWriter(sw);
        for (int i = 0; i < count; i++) {
            final MessagePrinter<MetricValue> valuePrinter = metricValuePrinter(line, entryId, entry, i);
            final MetricValue value = metricValue(line, entryId, entry, i);
            valuePrinter.print(line, entryId, value, pw);
        }
        pw.flush();
        return sw.toString();
    }

    @Override
    default Object value(final String placeholder, final long line, final long entryId, final MetricsLogEntry entry) {
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
            case METRICS_COUNT: return metricsCount(entryId, entryId, entry);
            case METRICS_VALUES: return metricsValues(entryId, entryId, entry);
            default: return placeholder;
        }
    }

    interface ValueFormatter extends org.tools4j.elara.format.ValueFormatter<MetricValue> {
        ValueFormatter DEFAULT = new ValueFormatter() {};

        /** Placeholder in metrics-values string for the name of a metric */
        String METRIC_NAME = "{metric-name}";
        /** Placeholder in metrics-values string for the value of a metric */
        String METRIC_VALUE = "{metric-value}";
        /** Placeholder in metrics-values string for the index of a metric */
        String METRIC_INDEX = "{metric-index}";

        default Object line(long line, long entryId, MetricValue value) {return line;}
        default Object entryId(long line, long entryId, MetricValue value) {return entryId;}
        default Object metricName(long line, long entryId, MetricValue value) {
            final Enum<?> name = value.name();
            if (name instanceof TimeMetric) {
                switch ((TimeMetric)name) {
                    case INPUT_SENDING_TIME: return "inp-snd";
                    case INPUT_POLLING_TIME: return "inp-rcv";
                    case COMMAND_APPENDING_TIME: return "cmd-apd";
                    case COMMAND_POLLING_TIME: return "cmd-pol";
                    case PROCESSING_START_TIME: return "cmd-beg";
                    case ROUTING_START_TIME: return "evt-beg";
                    case ROUTING_END_TIME: return "evt-rte";
                    case EVENT_POLLING_TIME: return "evt-pol";
                    case OUTPUT_POLLING_TIME: return "out-pol";
                    case APPLYING_START_TIME: return "evt-apy";
                    case APPLYING_END_TIME: return "evt-end";
                    case PROCESSING_END_TIME: return "cmd-end";
                    case OUTPUT_START_TIME: return "out-pub";
                    case OUTPUT_END_TIME: return "out-end";
                    case METRIC_APPENDING_TIME: return "met-apd";
                }
            } else if (name instanceof FrequencyMetric) {
                switch ((FrequencyMetric)name) {
                    case DUTY_CYCLE_FREQUENCY: return "cyc-all";
                    case INPUTS_POLL_FREQUENCY: return "inp-all";
                    case COMMAND_POLL_FREQUENCY: return "cmd-all";
                    case EVENT_POLL_FREQUENCY: return "evt-all";
                    case OUTPUT_POLL_FREQUENCY: return "out-all";
                    case EXTRA_STEP_INVOCATION_FREQUENCY: return "xta-all";

                    case DUTY_CYCLE_PERFORMED_FREQUENCY: return "cyc-prf";
                    case INPUT_RECEIVED_FREQUENCY: return "inp-rcv";
                    case COMMAND_PROCESSED_FREQUENCY: return "cmd-prc";
                    case EVENT_APPLIED_FREQUENCY: return "evt-apy";
                    case OUTPUT_PUBLISHED_FREQUENCY: return "out-pub";
                    case EXTRA_STEP_PERFORMED_FREQUENCY: return "xta-prf";
                    case STEP_ERROR_FREQUENCY: return "stp-err";
                }
            }
            return name;
        }
        default Object metricValue(long line, long entryId, MetricValue value) {return value.value();}
        default Object metricIndex(long line, long entryId, MetricValue value) {return value.index();}

        @Override
        default Object value(String placeholder, long line, long entryId, MetricValue value) {
            switch (placeholder) {
                case LINE_SEPARATOR: return System.lineSeparator();
                case MESSAGE: return value;
                case LINE: return line(line, entryId, value);
                case ENTRY_ID: return entryId(line, entryId, value);
                case METRIC_NAME: return metricName(line, entryId, value);
                case METRIC_VALUE: return metricValue(line, entryId, value);
                case METRIC_INDEX: return metricIndex(line, entryId, value);
                default: return placeholder;
            }
        }
    }

    default MessagePrinter<MetricValue> metricValuePrinter(long line, long entryId, MetricsLogEntry entry, int index) {
        final String format = index == 0 ? METRICS_VALUE_FORMAT_0 : METRICS_VALUE_FORMAT_N;
        final ValueFormatter formatter = metricValueFormatter(line, entryId, entry, index);
        return new ParameterizedMessagePrinter<>(format, formatter);
    }

    default ValueFormatter metricValueFormatter(long line, long entryId, MetricsLogEntry entry, int index) {
        return ValueFormatter.DEFAULT;
    }

    default MetricValue metricValue(final long line, final long entryId, final MetricsLogEntry entry, final int index) {
        final Type type = entry.type();
        final Enum<?> name = type == Type.TIME ? entry.target().metric(entry.flags(), index) :
                FrequencyMetric.metric(entry.choice(), index);
        final long value = type == Type.TIME ? entry.time(index) : entry.counter(index);
        return new MetricValue() {
            @Override
            public Enum<?> name() {
                return name;
            }

            @Override
            public long value() {
                return value;
            }

            @Override
            public int index() {
                return index;
            }

            @Override
            public String toString() {
                return "MetricValue{name=" + name + ", value=" + value + ", index=" + index + "}";
            }
        };
    }
}
