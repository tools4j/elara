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
package org.tools4j.elara.plugin.replication;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ProcessorConfig;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.PluginFactory;
import org.tools4j.elara.app.factory.ProcessorFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.EventPollerStep;

import static java.util.Objects.requireNonNull;

class ReplicationInterceptor implements Interceptor {

    private final AppConfig appConfig;
    private final ProcessorConfig processorConfig;
    private final Configuration pluginConfig;
    private final PluginFactory pluginSingletons;
    private final ReplicationState replicationState;

    public ReplicationInterceptor(final AppConfig appConfig,
                                  final ProcessorConfig processorConfig,
                                  final Configuration pluginConfig,
                                  final PluginFactory pluginSingletons,
                                  final ReplicationState replicationState) {
        this.appConfig = requireNonNull(appConfig);
        this.processorConfig = requireNonNull(processorConfig);
        this.pluginConfig = requireNonNull(pluginConfig);
        this.pluginSingletons = requireNonNull(pluginSingletons);
        this.replicationState = requireNonNull(replicationState);
    }

    @Override
    public ProcessorFactory interceptOrNull(final ProcessorFactory original) {
        requireNonNull(original);
        return new ProcessorFactory() {
            @Override
            public CommandProcessor commandProcessor() {
                return original.commandProcessor();
            }

            @Override
            public CommandHandler commandHandler() {
                return new ReplicationCommandHandler(
                        appConfig.timeSource(),
                        pluginConfig,
                        pluginSingletons.baseState(),
                        replicationState,
                        new DefaultEventRouter(
                                appConfig.timeSource(), processorConfig.eventStore().appender(), eventHandler()
                        ),
                        commandProcessor(),
                        appConfig.exceptionHandler(),
                        appConfig.duplicateHandler()
                );
            }

            @Override
            public EventApplier eventApplier() {
                return original.eventApplier();
            }

            @Override
            public EventHandler eventHandler() {
                return original.eventHandler();
            }

            @Override
            public AgentStep eventPollerStep() {
                return new EventPollerStep(processorConfig.eventStore().poller(), eventHandler());
            }
        };
    }
}
