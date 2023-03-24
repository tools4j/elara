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

import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Frequency metrics are counters for actions inside the elara engine over a configurable duration.  Some counters are
 * directly associated with application objects such as the number of commands or events processed over a certain period
 * while other metrics reflect intrinsic measurements like the number of duty cycle loop invocations.
 */
public enum FrequencyMetric implements Metric {
    /* duty cycle step invocation */
    DUTY_CYCLE_FREQUENCY("cyc-all"),
    INPUTS_POLL_FREQUENCY("inp-all"),
    COMMAND_POLL_FREQUENCY("cmd-all"),
    EVENT_POLL_FREQUENCY("evt-all"),
    OUTPUT_POLL_FREQUENCY("out-all"),
    EXTRA_STEP_INVOCATION_FREQUENCY("xtr-all"),

    /* duty cycle step performed work */
    DUTY_CYCLE_PERFORMED_FREQUENCY("cyc-prf"),
    INPUT_RECEIVED_FREQUENCY("inp-rcv"),
    COMMAND_PROCESSED_FREQUENCY("cmd-prc"),
    EVENT_POLLED_FREQUENCY("evt-pol"),
    OUTPUT_POLLED_FREQUENCY("out-pol"),
    EXTRA_STEP_PERFORMED_FREQUENCY("xtr-prf"),

    /* some other special ones*/
    EVENT_APPLIED_FREQUENCY("evt-apy"),
    OUTPUT_PUBLISHED_FREQUENCY("out-pub"),//only those where Output did not return IGNORED
    STEP_ERROR_FREQUENCY("stp-err");

    private final String displayName;

    FrequencyMetric(final String displayName) {
        this.displayName = requireNonNull(displayName);
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public Type type() {
        return Type.FREQUENCY;
    }

    private static final FrequencyMetric[] VALUES = values();
    private static final short ALL_FLAGS = (short)((1 << VALUES.length) - 1);
    static {
        assert VALUES.length == Integer.bitCount(ALL_FLAGS);
    }

    public static short choice(final FrequencyMetric... metrics) {
        short choice = 0;
        for (final FrequencyMetric metric : metrics) {
            choice |= (1 << metric.ordinal());
        }
        return choice;
    }

    public static short choice(final Set<FrequencyMetric> metrics) {
        short choice = 0;
        for (final FrequencyMetric metric : VALUES) {
            if (metrics.contains(metric)) {
                choice |= (1 << metric.ordinal());
            }
        }
        return choice;
    }

    public static boolean contains(final short choice, final FrequencyMetric metric) {
        return (choice & (1 << metric.ordinal())) != 0;
    }

    public static int count(final short choice) {
        return Integer.bitCount(ALL_FLAGS & choice);
    }

    public static FrequencyMetric metric(final short choice, final int index) {
        int c = ALL_FLAGS & choice;
        int count = 0;
        for (int i = 0; i < VALUES.length && c != 0; i++) {
            if ((c & 0x1) != 0) {
                if (count == index) {
                    return VALUES[i];
                }
                count++;
            }
            c >>>= 1;
        }
        return null;
    }

    public static int length() {
        return VALUES.length;
    }

    public static FrequencyMetric byOrdinal(final int ordinal) {
        return VALUES[ordinal];
    }
}
