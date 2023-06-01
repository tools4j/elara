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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.plugin.api.PluginStateProvider;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.replication.Connection.Handler;
import org.tools4j.elara.plugin.replication.ReplicationState.Mutable;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Appender;

import static java.util.Objects.requireNonNull;

/**
 * A plugin that issues a commands and events related to booting an elara application to indicate that the application
 * has been started and initialised.
 */
public class ReplicationPlugin implements SystemPlugin<ReplicationState.Mutable> {

    private final ReplicationConfig config;
    private final Specification specification = new Specification();

    public ReplicationPlugin(final ReplicationConfig config) {
        this.config = ReplicationConfig.validate(config);
    }

    public ReplicationConfig config() {
        return config;
    }

    @Override
    public SystemPluginSpecification<Mutable> specification() {
        return specification;
    }

    public static ReplicationContext configure() {
        return ReplicationContext.create();
    }

    public boolean isLeader(final ReplicationState state) {
        return state.leaderId() == config.serverId();
    }

    private final class Specification implements SystemPluginSpecification<ReplicationState.Mutable> {
        @Override
        public PluginStateProvider<Mutable> defaultPluginStateProvider() {
            return appConfig -> new DefaultReplicationState();
        }

        @Override
        public ReservedPayloadType reservedPayloadType() {
            return ReservedPayloadType.REPLICATION;
        }


        @Override
        public Installer installer(final AppConfig appConfig,
                                   final Mutable replicationState) {
            requireNonNull(appConfig);
            requireNonNull(replicationState);
            if (!(appConfig instanceof EventStoreConfig)) {
                throw new IllegalArgumentException("Plugin requires EventStoreConfig but found " + appConfig.getClass());
            }
            final EventStoreConfig eventStoreConfig = (EventStoreConfig) appConfig;
            final MessageStore eventStore = eventStoreConfig.eventStore();
            final Appender eventStoreAppender = eventStore.appender();
            final EnforcedLeaderEventReceiver enforcedLeaderEventReceiver = new EnforcedLeaderEventReceiver(
                    appConfig.loggerFactory(), appConfig.timeSource(), config, replicationState, eventStoreAppender
            );
            final DispatchingPublisher dispatchingPublisher = new DispatchingPublisher(config);
            final EventSender eventSender = new DefaultEventSender(config, replicationState, eventStore,
                    dispatchingPublisher);

            return new Installer.Default() {
                @Override
                public AgentStep step(final BaseState baseState, final ExecutionType executionType) {
                    //noinspection SwitchStatementWithTooFewBranches
                    switch (executionType) {
                        case ALWAYS_WHEN_EVENTS_APPLIED:
                            final Handler connectionHandler = new ConnectionHandler(
                                    appConfig.loggerFactory(), config, baseState, replicationState, eventStoreAppender, dispatchingPublisher
                            );
                            return new ReplicationPluginStep(
                                    config, replicationState, enforcedLeaderEventReceiver, connectionHandler, eventSender
                            );
                        default:
                            return AgentStep.NOOP;
                    }
                }

                @Override
                public CommandProcessor commandProcessor(final BaseState baseState) {
                    return new ReplicationCommandProcessor(appConfig.loggerFactory(), config, replicationState);
                }

                @Override
                public EventApplier eventApplier(final MutableBaseState baseState) {
                    return new ReplicationEventApplier(appConfig.loggerFactory(), config, replicationState);
                }

                @Override
                public Interceptor interceptor(final StateFactory stateFactory) {
                    return new ReplicationInterceptor(ReplicationPlugin.this, eventStoreConfig, stateFactory,
                            replicationState);
                }
            };
        }
    }
}
