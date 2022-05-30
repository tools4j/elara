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
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.ProcessorConfig;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.api.TypeRange;
import org.tools4j.elara.plugin.base.BaseState;
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

    private final org.tools4j.elara.plugin.replication.Configuration configuration;

    public ReplicationPlugin(final org.tools4j.elara.plugin.replication.Configuration configuration) {
        this.configuration = org.tools4j.elara.plugin.replication.Configuration.validate(configuration);
    }

    public static Context configure() {
        return Context.create();
    }

    @Override
    public TypeRange typeRange() {
        return TypeRange.REPLICATION;
    }

    @Override
    public ReplicationState.Mutable defaultPluginState() {
        return new DefaultReplicationState();
    }

    @Override
    public Configuration configuration(final AppConfig appConfig,
                                       final Mutable replicationState) {
        requireNonNull(appConfig);
        requireNonNull(replicationState);
        if (!(appConfig instanceof ProcessorConfig)) {
            throw new IllegalArgumentException("Plugin requires ProcessorConfig but found " + appConfig.getClass());
        }
        final ProcessorConfig processorConfig = (ProcessorConfig) appConfig;
        final MessageStore eventStore = processorConfig.eventStore();
        final Appender eventStoreAppender = eventStore.appender();
        final EnforcedLeaderEventReceiver enforcedLeaderEventReceiver = new EnforcedLeaderEventReceiver(
                appConfig.loggerFactory(), appConfig.timeSource(), configuration, replicationState, eventStoreAppender
        );
        final DispatchingPublisher dispatchingPublisher = new DispatchingPublisher(configuration);
        final EventSender eventSender = new DefaultEventSender(configuration, replicationState, eventStore,
                dispatchingPublisher);

        return new Configuration.Default() {
            @Override
            public AgentStep step(final BaseState baseState, final ExecutionType executionType) {
                //noinspection SwitchStatementWithTooFewBranches
                switch (executionType) {
                    case ALWAYS_WHEN_EVENTS_APPLIED:
                        final Handler connectionHandler = new ConnectionHandler(
                                appConfig.loggerFactory(), configuration, baseState, replicationState, eventStoreAppender, dispatchingPublisher
                        );
                        return new ReplicationPluginStep(
                                configuration, replicationState, enforcedLeaderEventReceiver, connectionHandler, eventSender
                        );
                    default:
                        return AgentStep.NOOP;
                }
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new ReplicationCommandProcessor(appConfig.loggerFactory(), configuration, replicationState);
            }

            @Override
            public EventApplier eventApplier(final BaseState.Mutable baseState) {
                return new ReplicationEventApplier(appConfig.loggerFactory(), configuration, replicationState);
            }

            @Override
            public Interceptor interceptor(final BaseState.Mutable baseState) {
                return new ReplicationInterceptor(appConfig, processorConfig, configuration, baseState, replicationState);
            }
        };
    }
}
