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
import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.app.factory.ApplierFactory;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.ProcessorFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.EventPollerStep;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class ReplicationInterceptor implements Interceptor {

    private final AppConfig appConfig;
    private final EventStoreConfig eventStoreConfig;
    private final Configuration pluginConfig;
    private final BaseState.Mutable baseState;
    private final ReplicationState replicationState;

    private Supplier<? extends ApplierFactory> eventApplierSingletons;

    public ReplicationInterceptor(final AppConfig appConfig,
                                  final EventStoreConfig eventStoreConfig,
                                  final Configuration pluginConfig,
                                  final BaseState.Mutable baseState,
                                  final ReplicationState replicationState) {
        this.appConfig = requireNonNull(appConfig);
        this.eventStoreConfig = requireNonNull(eventStoreConfig);
        this.pluginConfig = requireNonNull(pluginConfig);
        this.baseState = requireNonNull(baseState);
        this.replicationState = requireNonNull(replicationState);
    }

    @Override
    public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
        eventApplierSingletons = requireNonNull(singletons);
        return new ApplierFactory() {
            @Override
            public EventApplier eventApplier() {
                return singletons.get().eventApplier();
            }

            @Override
            public EventHandler eventHandler() {
                return singletons.get().eventHandler();
            }

            @Override
            public AgentStep eventPollerStep() {
                //NOTE: need override here since default only polls during replay phase
                return new EventPollerStep(eventStoreConfig.eventStore().poller(), eventHandler());
            }
        };
    }

    @Override
    public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
        requireNonNull(singletons);
        return new ProcessorFactory() {
            @Override
            public CommandProcessor commandProcessor() {
                return singletons.get().commandProcessor();
            }

            @Override
            public CommandHandler commandHandler() {
                requireNonNull(eventApplierSingletons, "eventApplierSingletons is null");
                return new ReplicationCommandHandler(
                        appConfig.timeSource(),
                        pluginConfig,
                        baseState,
                        replicationState,
                        new DefaultEventRouter(
                                appConfig.timeSource(), eventStoreConfig.eventStore().appender(),
                                eventApplierSingletons.get().eventHandler()
                        ),
                        commandProcessor(),
                        appConfig.exceptionHandler(),
                        eventStoreConfig.duplicateHandler()
                );
            }
        };
    }
}
