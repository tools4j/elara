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

public enum FrequencyMetric {
    /* duty cycle step invocation */
    DUTY_CYCLE_FREQUENCY,
    INPUTS_POLL_FREQUENCY,
    COMMAND_POLL_FREQUENCY,
    EVENT_POLL_FREQUENCY,
    OUTPUT_POLL_FREQUENCY,
    EXTRA_STEP_INVOCATION_FREQUENCY,

    /* duty cycle step performed work */
    DUTY_CYCLE_PERFORMED_FREQUENCY,
    INPUT_RECEIVED_FREQUENCY,
    COMMAND_PROCESSED_FREQUENCY,
    EVENT_APPLIED_FREQUENCY,
    OUTPUT_POLLED_FREQUENCY,
    EXTRA_STEP_PERFORMED_FREQUENCY,

    /* some other special ones*/
    OUTPUT_PUBLISHED_FREQUENCY,//only those where Output did not return IGNORED
    STEP_ERROR_FREQUENCY;

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

    public static int count() {
        return VALUES.length;
    }

    public static FrequencyMetric byOrdinal(final int ordinal) {
        return VALUES[ordinal];
    }
}
