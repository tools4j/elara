/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.factory.InterceptableSingletons;
import org.tools4j.elara.factory.Singletons;
import org.tools4j.elara.init.ExecutionType;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.api.TypeRange;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.replication.Connection.Handler;
import org.tools4j.nobark.loop.Step;

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
    public Configuration configuration(final org.tools4j.elara.init.Configuration appConfig,
                                       final ReplicationState.Mutable replicationState) {
        requireNonNull(appConfig);
        requireNonNull(replicationState);
        final MessageLog eventLog = appConfig.eventLog();
        final Appender eventLogAppender = eventLog.appender();
        final EnforcedLeaderEventReceiver enforcedLeaderEventReceiver = new EnforcedLeaderEventReceiver(
                appConfig.loggerFactory(), appConfig.timeSource(), configuration, replicationState, eventLogAppender
        );
        final DispatchingPublisher dispatchingPublisher = new DispatchingPublisher(configuration);
        final EventSender eventSender = new DefaultEventSender(configuration, replicationState, eventLog,
                dispatchingPublisher);

        return new Configuration.Default() {
            @Override
            public Step step(final BaseState baseState, final ExecutionType executionType) {
                switch (executionType) {
                    case ALWAYS_WHEN_EVENTS_APPLIED:
                        final Handler connectionHandler = new ConnectionHandler(
                                appConfig.loggerFactory(), configuration, baseState, replicationState, eventLogAppender, dispatchingPublisher
                        );
                        return new ReplicationPluginStep(
                                configuration, replicationState, enforcedLeaderEventReceiver, connectionHandler, eventSender
                        );
                    default:
                        return Step.NO_OP;
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
            public InterceptableSingletons interceptOrNull(final Singletons singletons) {
                return new ReplicationInterceptor(singletons, appConfig, configuration, replicationState);
            }
        };
    }
}
