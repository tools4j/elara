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

import org.junit.jupiter.api.Test;
import org.tools4j.elara.plugin.metrics.Metric.Type;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.plugin.metrics.LatencyMetric.values;

public class LatemcyMetricTest {
    
    private static final LatencyMetric[] VALUES = values();

    @Test
    public void displayNameUnique() {
        //when
        final Set<String> displayName = Arrays.stream(VALUES).map(LatencyMetric::displayName).collect(Collectors.toSet());

        //then
        assertEquals(VALUES.length, displayName.size());
    }

    @Test
    public void typeIsLatency() {
        for (final LatencyMetric metric : VALUES) {
            assertEquals(Type.LATENCY, metric.type());
        }
    }

    @Test
    public void validStartAndEnd() {
        final Set<Set<TimeMetric>> startEndSet = new HashSet<>();
        for (final LatencyMetric metric : VALUES) {
            assertNotNull(metric.start());
            assertNotNull(metric.end());
            assertNotEquals(metric.start(), metric.end());
            assertTrue(metric.start().ordinal() < metric.end().ordinal());
            assertTrue(metric.involves(metric.start()));
            assertTrue(metric.involves(metric.end()));
            for (final TimeMetric timeMetric : TimeMetric.values()) {
                assertEquals(metric.start() == timeMetric || metric.end() == timeMetric,
                        metric.involves(timeMetric));
            }
            startEndSet.add(EnumSet.of(metric.start(), metric.end()));
        }
        assertEquals(VALUES.length, startEndSet.size());
    }
}
