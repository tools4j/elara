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

import org.junit.jupiter.api.Test;
import org.tools4j.elara.plugin.metrics.Metric.Type;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyMetricTest {

    private static final FrequencyMetric[] VALUES = FrequencyMetric.values();

    @Test
    public void displayNameUnique() {
        //when
        final Set<String> displayName = Arrays.stream(VALUES).map(FrequencyMetric::displayName).collect(Collectors.toSet());

        //then
        assertEquals(VALUES.length, displayName.size());
    }

    @Test
    public void typeIsFrequency() {
        for (final FrequencyMetric metric : VALUES) {
            assertEquals(Type.FREQUENCY, metric.type());
        }
    }

    @Test
    public void choiceUniqueForSingleMetric() {
        //given
        final Set<Short> choices = new HashSet<>();

        //when
        for (final FrequencyMetric metric : VALUES) {
            //when
            final short choice = FrequencyMetric.choice(metric);

            //then
            assertEquals(metric, FrequencyMetric.metric(choice, 0));
            assertEquals(1, FrequencyMetric.count(choice));
            assertEquals(1, Integer.bitCount(Short.toUnsignedInt(choice)));
            assertTrue(choices.add(choice));
        }

        //then
        assertEquals(VALUES.length, choices.size());
    }

    @Test
    public void choiceFitsAllMetrics() {
        //when
        final short choice = FrequencyMetric.choice(VALUES);
        //then
        assertEquals(VALUES.length, FrequencyMetric.count());
        assertEquals(VALUES.length, Integer.bitCount(Short.toUnsignedInt(choice)));
        assertEquals(VALUES.length, FrequencyMetric.count(choice));
        assertEquals(Short.BYTES, MetricsDescriptor.CHOICE_LENGTH);
    }
}