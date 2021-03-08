/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Time metrics measure single points in time along processing of input, command, event and output flows. Each time
 * metric is associated with a command or event ID.
 * <p>
 * The different time metrics are defined as follows:
 *
 * <pre>{@code
 *                             Input --> (_) --> Command -+--> Event.0 --> (_) +-> State
 *                                                        `--> Event.1 --> (_) +-> State
 *                                                                             `---------> Output
 *
 *                            ^     ^   ^   ^   ^             ^        ^       ^   ^   ^           ^
 *  (input sending time)......'     |   |   |   |             |        |       |   |   |^          ^
 *  (input polling time)............'   |   |   |             |        |       |^  |   ||  ^    ^  ^
 *  (command appending time)............'   |   |             |        |       ||  |   ||  |    |  |
 *  (command polling time)..................'   |             |        |       ||  |   ||  |    |  |
 *  (processing start time).....................'             |        |       ||  |   ||  |    |  |
 *  (routing start time)......................................'        |       ||  |   ||  |    |  |
 *  (routing end time).................................................'       ||  |   ||  |    |  |
 *  (event polling time).......................................................'|  |   ||  |    |  |
 *  (output polling time).......................................................'  |   ||  |    |  |
 *  (applying start time)..........................................................'   ||  |    |  |
 *  (applying end time)................................................................'|  |    |  |
 *  (processing end time)...............................................................'  |    |  |
 *  (output start time)....................................................................'    |  |
 *  (output end time)...........................................................................'  |
 *  (metric appending time)........................................................................'
 *
 * }</pre>
 */
public enum TimeMetric implements Metric {
    INPUT_SENDING_TIME("inp-snd"),
    INPUT_POLLING_TIME("inp-rcv"),
    COMMAND_APPENDING_TIME("cmd-apd"),
    COMMAND_POLLING_TIME("cmd-pol"),
    PROCESSING_START_TIME("cmd-beg"),
    ROUTING_START_TIME("evt-beg"),
    ROUTING_END_TIME("evt-rte"),
    EVENT_POLLING_TIME("evt-pol"),
    OUTPUT_POLLING_TIME("out-pol"),
    APPLYING_START_TIME("evt-apy"),
    APPLYING_END_TIME("evt-end"),
    PROCESSING_END_TIME("cmd-end"),
    OUTPUT_START_TIME("out-beg"),
    OUTPUT_END_TIME("out-end"),
    METRIC_APPENDING_TIME("met-apd");

    private final String displayName;

    TimeMetric(final String displayName) {
        this.displayName = requireNonNull(displayName);
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Type type() {
        return Type.TIME;
    }

    public enum Target {
        COMMAND(INPUT_SENDING_TIME, INPUT_POLLING_TIME, COMMAND_APPENDING_TIME, COMMAND_POLLING_TIME,
                PROCESSING_START_TIME, PROCESSING_END_TIME, METRIC_APPENDING_TIME),
        EVENT(ROUTING_START_TIME, ROUTING_END_TIME, EVENT_POLLING_TIME,
                APPLYING_START_TIME, APPLYING_END_TIME, METRIC_APPENDING_TIME),
        OUTPUT(OUTPUT_POLLING_TIME, OUTPUT_START_TIME, OUTPUT_END_TIME, METRIC_APPENDING_TIME);

        private static final byte OUTPUT_BIT = 0b1000000;
        private static final int[] BIT_COUNT = {
                0, 1, 1, 2, //0-3
                1, 2, 2, 3, //4-7
                1, 2, 2, 3, 2, 3, 3, 4, //8-15
        };

        private final byte allFlags;
        private final TimeMetric[] metrics;
        private final byte[] metricFlagByOrdinal = new byte[VALUES.length];
        Target(final TimeMetric... metrics) {
            this.metrics = requireNonNull(metrics);
            byte all = 0;
            for (int i = 0; i < metrics.length; i++) {
                final byte flag = (byte)(1 << i);
                all |= flag;
                metricFlagByOrdinal[metrics[i].ordinal()] = flag;
            }
            this.allFlags = all;
        }

        public byte flag(final TimeMetric metric) {
            return metricFlagByOrdinal[metric.ordinal()];
        }

        public byte flags(final Set<TimeMetric> metrics) {
            byte flags = this == OUTPUT ? OUTPUT_BIT : 0;
            for (final TimeMetric metric : metrics) {
                if (metrics.contains(metric)) {
                    flags |= flag(metric);
                }
            }
            return flags;
        }

        public boolean isMetric(final TimeMetric metric) {
            return flag(metric) != 0;
        }

        public boolean both(final TimeMetric first, final TimeMetric second) {
            return isMetric(first) && isMetric(second);
        }
        public boolean anyOf(final Set<TimeMetric> set) {
            for (final TimeMetric metric : metrics) {
                if (set.contains(metric)) {
                    return true;
                }
            }
            return false;
        }

        public int count(final byte flags) {
            final int f = allFlags & flags;
            return BIT_COUNT[0x0f & f] + BIT_COUNT[0x0f & (f >>> 4)];
        }

        public boolean contains(final byte flags, final TimeMetric metric) {
            final int f = allFlags & flags;
            return (f & metricFlagByOrdinal[metric.ordinal()]) != 0;
        }

        public TimeMetric metric(final byte flags, final int index) {
            int f = allFlags & flags;
            int count = 0;
            for (int i = 0; i < metrics.length && f != 0; i++) {
                if ((f & 0x1) != 0) {
                    if (count == index) {
                        return metrics[i];
                    }
                    count++;
                }
                f >>>= 1;
            }
            return null;
        }

        public static Target target(final byte flags, final int index) {
            if (index >= 0) {
                //event or output
                return (OUTPUT_BIT & flags) != 0 ? OUTPUT : EVENT;
            }
            return COMMAND;
        }
    }

    private static final TimeMetric[] VALUES = values();

    public static int count() {
        return VALUES.length;
    }

    public static TimeMetric byOrdinal(final int ordinal) {
        return VALUES[ordinal];
    }

}
