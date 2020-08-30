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

import org.tools4j.elara.factory.InterceptableSingletons;
import org.tools4j.elara.factory.Singletons;
import org.tools4j.elara.init.ExecutionType;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.nobark.loop.Step;

import static java.util.Objects.requireNonNull;

/**
 * A plugin that captures configurable measurements such as latencies or counts/frequencies.
 */
public class MetricsPlugin implements Plugin<MetricsState> {

    private final org.tools4j.elara.plugin.metrics.Configuration configuration;

    public MetricsPlugin(final org.tools4j.elara.plugin.metrics.Configuration configuration) {
        this.configuration = org.tools4j.elara.plugin.metrics.Configuration.validate(configuration);
    }

    @Override
    public MetricsState defaultPluginState() {
        return new DefaultMetricsState();
    }

    @Override
    public Configuration configuration(final org.tools4j.elara.init.Configuration appConfig, final MetricsState pluginState) {
        requireNonNull(appConfig);
        requireNonNull(pluginState);
        return new Configuration.Default() {
            @Override
            public Step step(final BaseState baseState, final ExecutionType executionType) {
                if (configuration.frequencyMetrics().isEmpty() || executionType != ExecutionType.ALWAYS) {
                    return Step.NO_OP;
                }
                return new FrequencyMetricsLoggerStep(appConfig.timeSource(), configuration, pluginState);
            }

            @Override
            public InterceptableSingletons interceptOrNull(final Singletons singletons) {
                return new MetricsCapturingInterceptor(singletons, appConfig.timeSource(), configuration, pluginState);
            }
        };
    }
}
