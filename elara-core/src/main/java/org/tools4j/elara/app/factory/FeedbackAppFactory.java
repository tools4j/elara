/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.factory;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.agent.FeedbackAgent;
import org.tools4j.elara.app.state.EventProcessingState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.app.state.MutableEventProcessingState;
import org.tools4j.elara.app.type.FeedbackAppConfig;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.tools4j.elara.app.factory.Bootstrap.bootstrap;

public class FeedbackAppFactory implements AppFactory {
    private final EventSubscriberFactory eventSubscriberSingletons;
    private final EventProcessorFactory eventProcessorSingletons;
    private final CommandSenderFactory commandSenderSingletons;
    private final InputFactory inputSingletons;
    private final OutputFactory outputSingletons;
    private final AgentStepFactory agentStepSingletons;
    private final AppFactory appSingletons;

    public FeedbackAppFactory(final FeedbackAppConfig config) {
        final Bootstrap bootstrap = bootstrap(config, config);
        final Interceptor interceptor = bootstrap.interceptor();
        final MutableEventProcessingState eventProcessingState = eventProcessingState(bootstrap);
        this.eventSubscriberSingletons = interceptor.eventSubscriberFactory(singletonsSupplier(
                (EventSubscriberFactory) new DefaultEventSubscriberFactory(config, config, eventProcessingState, this::eventSubscriberSingletons, this::eventProcessorSingletons),
                Singletons::create
        ));
        this.eventProcessorSingletons = interceptor.eventProcessorFactory(singletonsSupplier(
                (EventProcessorFactory) new DefaultEventProcessorFactory(config, config, eventProcessingState, bootstrap.plugins(), this::eventProcessorSingletons, this::commandSenderSingletons, this::outputSingletons),
                Singletons::create
        ));
        this.commandSenderSingletons = interceptor.commandSenderFactory(singletonsSupplier(
                (CommandSenderFactory) new CommandMessageSenderFactory(config, config, bootstrap.baseState(), this::commandSenderSingletons),
                Singletons::create
        ));
        this.inputSingletons = interceptor.inputFactory(singletonsSupplier(
                (InputFactory) new DefaultInputFactory(config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.outputSingletons = interceptor.outputFactory(singletonsSupplier(
                (OutputFactory) new DefaultOutputFactory(config, config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.agentStepSingletons = interceptor.agentStepFactory(singletonsSupplier(
                (AgentStepFactory)new DefaultAgentStepFactory(config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.appSingletons = interceptor.appFactory(singletonsSupplier(
                appFactory(eventProcessingState), Singletons::create
        ));
    }

    private static MutableEventProcessingState eventProcessingState(final Bootstrap bootstrap) {
        final MutableBaseState baseState = bootstrap.baseState();
        if (baseState instanceof MutableEventProcessingState) {
            return (MutableEventProcessingState)baseState;
        }
        throw new IllegalStateException("Base state for FeedbackApp must be an instance of " +
                MutableEventProcessingState.class.getSimpleName() + " but was " + baseState.getClass().getSimpleName());
    }

    private <T> Supplier<T> singletonsSupplier(final T factory, final UnaryOperator<T> singletonOp) {
        final T singletons = singletonOp.apply(factory);
        return () -> singletons;
    }

    private EventSubscriberFactory eventSubscriberSingletons() {
        return eventSubscriberSingletons;
    }

    private EventProcessorFactory eventProcessorSingletons() {
        return eventProcessorSingletons;
    }

    private CommandSenderFactory commandSenderSingletons() {
        return commandSenderSingletons;
    }

    private OutputFactory outputSingletons() {
        return outputSingletons;
    }

    private AppFactory appFactory(final EventProcessingState eventProcessingState) {
        return () -> new FeedbackAgent(
                eventProcessingState,
                commandSenderSingletons.commandContext(),
                inputSingletons.input().inputPollerStep(commandSenderSingletons.commandContext()),
                eventSubscriberSingletons.playbackPollerStep(),
                agentStepSingletons.extraStepAlwaysWhenEventsApplied(),
                agentStepSingletons.extraStepAlways());
    }

    @Override
    public Agent agent() {
        return appSingletons.agent();
    }
}
