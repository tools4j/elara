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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.plugin.api.PluginStateProvider;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.step.AgentStep;

import static java.util.Objects.requireNonNull;

/**
 * A plugin that captures configurable measurements such as latencies or counts/frequencies.
 */
public class MetricsPlugin implements SystemPlugin<MetricsState> {

    private final MetricsConfig config;
    private final Specification specification = new Specification();

    public MetricsPlugin(final MetricsConfig config) {
        this.config = MetricsConfig.validate(config);
    }

    public MetricsConfig config() {
        return config;
    }

    @Override
    public SystemPluginSpecification<MetricsState> specification() {
        return specification;
    }

    public static MetricsContext configure() {
        return MetricsConfig.configure();
    }

    private final class Specification implements SystemPluginSpecification<MetricsState> {

        @Override
        public PluginStateProvider<MetricsState> defaultPluginStateProvider() {
            return appConfig -> new DefaultMetricsState();
        }

        @Override
        public ReservedPayloadType reservedPayloadType() {
            return ReservedPayloadType.NONE;
        }

        @Override
        public Installer installer(final AppConfig appConfig, final MetricsState pluginState) {
            requireNonNull(appConfig);
            requireNonNull(pluginState);
            return new Installer.Default() {
                @Override
                public AgentStep step(final BaseState baseState, final ExecutionType executionType) {
                    if (config.frequencyMetrics().isEmpty() || executionType != ExecutionType.ALWAYS) {
                        return AgentStep.NOOP;
                    }
                    return new FrequencyMetricsWriterStep(appConfig.timeSource(), config, pluginState);
                }

                @Override
                public Interceptor interceptor(final StateFactory stateFactory) {
                    return new MetricsCapturingInterceptor(appConfig.timeSource(), config, pluginState);
                }
            };
        }
    }
}