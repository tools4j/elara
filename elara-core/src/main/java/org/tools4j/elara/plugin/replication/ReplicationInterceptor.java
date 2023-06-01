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
package org.tools4j.elara.plugin.replication;

import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.app.factory.ApplierFactory;
import org.tools4j.elara.app.factory.CommandPollerFactory;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.EventPollerStep;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Poller;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class ReplicationInterceptor implements Interceptor {

    private final ReplicationPlugin plugin;
    private final EventStoreConfig eventStoreConfig;
    private final StateFactory stateFactory;
    private final ReplicationState replicationState;

    public ReplicationInterceptor(final ReplicationPlugin plugin,
                                  final EventStoreConfig eventStoreConfig,
                                  final StateFactory stateFactory,
                                  final ReplicationState replicationState) {
        this.plugin = requireNonNull(plugin);
        this.eventStoreConfig = requireNonNull(eventStoreConfig);
        this.stateFactory = requireNonNull(stateFactory);
        this.replicationState = requireNonNull(replicationState);
    }

    @Override
    public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
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
    public CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
        requireNonNull(singletons);
        return new CommandPollerFactory() {
            @Override
            public Poller commandMessagePoller() {
                return singletons.get().commandMessagePoller();
            }

            @Override
            public Handler commandMessageHandler() {
                final Handler leaderHandler = singletons.get().commandMessageHandler();
                return new ReplicationCommandHandler(plugin, stateFactory.baseState(), replicationState, leaderHandler);
            }

            @Override
            public AgentStep commandPollerStep() {
                return singletons.get().commandPollerStep();
            }
        };
    }
}
