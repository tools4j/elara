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

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.base.BaseState.Mutable;

import java.util.EnumSet;

import static java.util.Objects.requireNonNull;

/**
 * A plugin that performs configurable measurements such as latencies.
 */
public class MetricsPlugin implements Plugin<MetricsState> {

    private final EnumSet<TimeMetric> timeMetrics;
    private final EnumSet<FrequencyMetric> frequencyMetrics;

    public MetricsPlugin(final TimeMetric... timeMetrics) {
        this(EnumSet.of(timeMetrics[0], timeMetrics), EnumSet.noneOf(FrequencyMetric.class));
    }

    public MetricsPlugin(final FrequencyMetric... frequencyMetrics) {
        this(EnumSet.noneOf(TimeMetric.class), EnumSet.of(frequencyMetrics[0], frequencyMetrics));
    }

    public MetricsPlugin(final EnumSet<TimeMetric> timeMetrics, final EnumSet<FrequencyMetric> frequencyMetrics) {
        this.timeMetrics = requireNonNull(timeMetrics);
        this.frequencyMetrics = requireNonNull(frequencyMetrics);
    }

    @Override
    public MetricsState defaultPluginState() {
        return new DefaultMetricsState(timeMetrics, frequencyMetrics);
    }

    @Override
    public Configuration configuration(final org.tools4j.elara.init.Configuration appConfig, final MetricsState pluginState) {
        requireNonNull(appConfig);
        requireNonNull(pluginState);
        return new Configuration.Default() {
            @Override
            public Input[] inputs(final BaseState baseState) {
                return new Input[0];
            }

            @Override
            public Output output(final BaseState baseState) {
                return Output.NOOP;
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return null;
            }

            @Override
            public EventApplier eventApplier(final Mutable baseState) {
                return EventApplier.NOOP;
            }
        };
    }
}
