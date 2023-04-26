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
package org.tools4j.elara.app.factory;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.agent.PassthroughAgent;
import org.tools4j.elara.app.config.ApplierConfig;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.app.state.PassthroughEventApplier;
import org.tools4j.elara.app.state.PassthroughState;
import org.tools4j.elara.app.type.PassthroughAppConfig;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.app.factory.Bootstrap.bootstrap;

public class PassthroughAppFactory implements AppFactory {
    private final SequencerFactory sequencerSingletons;
    private final ApplierFactory applierSingletons;
    private final InputFactory inputSingletons;
    private final OutputFactory outputSingletons;
    private final PublisherFactory publisherSingletons;
    private final AgentStepFactory agentStepSingletons;
    private final AppFactory appSingletons;

    public PassthroughAppFactory(final PassthroughAppConfig config) {
        final Bootstrap bootstrap = bootstrap(config, config);
        final Interceptor interceptor = bootstrap.interceptor();
        this.sequencerSingletons = interceptor.sequencerFactory(singletonsSupplier(
                (SequencerFactory) new PassthroughSequencerFactory(config, config, bootstrap.baseState(), this::sequencerSingletons, this::inputSingletons, this::applierSingletons),
                Singletons::create
        ));
        this.applierSingletons = interceptor.applierFactory(singletonsSupplier(
                (ApplierFactory) new DefaultApplierFactory(config, applierConfig(config, bootstrap.baseState()), config, bootstrap.baseState(), bootstrap.plugins(), this::applierSingletons),
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
        this.publisherSingletons = interceptor.publisherFactory(singletonsSupplier(
                (PublisherFactory)new DefaultPublisherFactory(config, config, bootstrap.baseState(), this::publisherSingletons, this::outputSingletons),
                Singletons::create
        ));
        this.agentStepSingletons = interceptor.agentStepFactory(singletonsSupplier(
                (AgentStepFactory)new DefaultAgentStepFactory(config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.appSingletons = interceptor.appFactory(singletonsSupplier(
                appFactory(), Singletons::create
        ));
    }

    private ApplierConfig applierConfig(final PassthroughAppConfig config, final MutableBaseState baseState) {
        requireNonNull(config);
        requireNonNull(baseState);
        final EventApplier eventApplier;
        if (config instanceof ApplierConfig) {
            eventApplier = ((ApplierConfig)config).eventApplier();
        } else {
            if (baseState instanceof PassthroughState) {
                final PassthroughState passthroughState = (PassthroughState) baseState;
                eventApplier = (PassthroughEventApplier) passthroughState::applyEvent;
            } else {
                eventApplier = baseState::applyEvent;
            }
        }
        return () -> eventApplier;
    }

    private <T> Supplier<T> singletonsSupplier(final T factory, final UnaryOperator<T> singletonOp) {
        final T singletons = singletonOp.apply(factory);
        return () -> singletons;
    }

    private ApplierFactory applierSingletons() {
        return applierSingletons;
    }

    private InputFactory inputSingletons() {
        return inputSingletons;
    }

    private OutputFactory outputSingletons() {
        return outputSingletons;
    }

    private SequencerFactory sequencerSingletons() {
        return sequencerSingletons;
    }

    private PublisherFactory publisherSingletons() {
        return publisherSingletons;
    }

    private AppFactory appFactory() {
        return () -> new PassthroughAgent(
                sequencerSingletons.sequencerStep(),
                applierSingletons.eventPollerStep(),
                publisherSingletons.publisherStep(),
                agentStepSingletons.extraStepAlwaysWhenEventsApplied(),
                agentStepSingletons.extraStepAlways());
    }

    @Override
    public Agent agent() {
        return appSingletons.agent();
    }
}
