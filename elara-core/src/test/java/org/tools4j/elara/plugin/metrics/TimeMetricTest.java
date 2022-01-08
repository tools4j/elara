/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.plugin.metrics.TimeMetric.METRIC_APPENDING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.values;

class TimeMetricTest {

    private static final TimeMetric[] VALUES = values();

    @Test
    public void displayNameUnique() {
        //when
        final Set<String> displayName = Arrays.stream(VALUES).map(TimeMetric::displayName).collect(Collectors.toSet());

        //then
        assertEquals(VALUES.length, displayName.size());
    }

    @Test
    public void typeIsTime() {
        for (final TimeMetric metric : VALUES) {
            assertEquals(Type.TIME, metric.type());
        }
    }

    @Test
    public void flagsUniqueForSingleMetric() {
        //given
        final Set<Byte> cmdFlags = new HashSet<>();
        final Set<Byte> evtFlags = new HashSet<>();

        for (final Target target : Target.values()) {
            for (final TimeMetric metric : VALUES) {
                if (target.isMetric(metric)) {
                    //when
                    final byte flags = target.flags(EnumSet.of(metric));

                    //then
                    assertEquals(metric, target.metric(flags, 0));
                    assertEquals(1, target.count(flags));
                    assertTrue(target == Target.OUTPUT ? flags < 0 : flags > 0);
                    assertEquals(target == Target.OUTPUT ? 2 : 1, Integer.bitCount(Byte.toUnsignedInt(flags)));
                    assertTrue((target == Target.COMMAND ? cmdFlags : evtFlags).add(flags));
                }
            }
        }

        //then
        assertEquals(VALUES.length + 2, cmdFlags.size() + evtFlags.size(), "each metric in one target, except one metric in all targets");
    }

    @Test
    public void flagsFitsAllMetricsPerTarget() {
        //when
        final Set<TimeMetric> captured = EnumSet.noneOf(TimeMetric.class);
        for (final Target target : Target.values()) {
            byte flags = 0;
            for (final TimeMetric metric : VALUES) {
                if (target.isMetric(metric)) {
                    flags |= target.flag(metric);
                    assertTrue(captured.add(metric) || metric == METRIC_APPENDING_TIME);
                }
            }

            //then
            assertEquals(Integer.bitCount(Byte.toUnsignedInt(flags)), target.count());
            assertTrue(flags > 0, "sign bit reserved to distinguish EVENT from OUTPUT");
        }

        //then
        assertEquals(VALUES.length, captured.size());
        assertEquals(Byte.BYTES, MetricsDescriptor.FLAGS_LENGTH);
    }
}