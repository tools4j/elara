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
package org.tools4j.elara.plugin.metrics;

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * <pre>{@code
 *                             Input --> (_) --> Command -+--> Event.0 --> (_) +-> State
 *                                                        `--> Event.1 --> (_) +-> State
 *                                                                             `---------> Output
 *
 *                            ^     ^   ^   ^   ^             ^       ^        ^   ^   ^           ^
 *  (input sending time)......'     |   |   |   |             |       |        |   |   |^          ^
 *  (input polling time)............'   |   |   |             |       |        |^  |   ||  ^    ^  ^
 *  (command appending time)............'   |   |             |       |        ||  |   ||  |    |  |
 *  (command polling time)..................'   |             |       |        ||  |   ||  |    |  |
 *  (processing start time).....................'             |       |        ||  |   ||  |    |  |
 *  (routing start time)......................................'       |        ||  |   ||  |    |  |
 *  (routing end time)................................................'        ||  |   ||  |    |  |
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
public enum TimeMetric {
    INPUT_SENDING_TIME,
    INPUT_POLLING_TIME,
    COMMAND_APPENDING_TIME,
    COMMAND_POLLING_TIME,
    PROCESSING_START_TIME,
    ROUTING_START_TIME,
    ROUTING_END_TIME,
    EVENT_POLLING_TIME,
    OUTPUT_POLLING_TIME,
    APPLYING_START_TIME,
    APPLYING_END_TIME,
    PROCESSING_END_TIME,
    OUTPUT_START_TIME,
    OUTPUT_END_TIME,
    METRIC_APPENDING_TIME;

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


        private final TimeMetric[] metrics;
        private final byte[] metricFlagByOrdinal = new byte[VALUES.length];
        Target(final TimeMetric... metrics) {
            this.metrics = requireNonNull(metrics);
            for (int i = 0; i < metrics.length; i++) {
                final byte flag = (byte)(1 << i);
                metricFlagByOrdinal[metrics[i].ordinal()] = flag;
            }
        }

        byte flag(final TimeMetric metric) {
            return metricFlagByOrdinal[metric.ordinal()];
        }

        byte flags(final Set<TimeMetric> metrics) {
            byte flags = this == OUTPUT ? OUTPUT_BIT : 0;
            for (final TimeMetric metric : VALUES) {
                if (metrics.contains(metric)) {
                    flags |= flag(metric);
                }
            }
            return flags;
        }

        boolean isMetric(final TimeMetric metric) {
            return flag(metric) != 0;
        }

        int count(final byte flags) {
            return BIT_COUNT[0x0f & flags] + BIT_COUNT[0x0f & (flags >>> 4)];
        }

        TimeMetric metric(final byte flags, final int index) {
            int f = 0xff & flags;
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

        static Target target(final byte flags, final int index) {
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
