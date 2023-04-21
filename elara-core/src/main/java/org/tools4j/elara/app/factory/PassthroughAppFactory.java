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
import org.tools4j.elara.app.type.PassthroughAppConfig;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.base.EventIdApplier;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public class PassthroughAppFactory implements AppFactory {

    private final PluginFactory pluginSingletons;
    private final SequencerFactory sequencerSingletons;
    private final ApplierFactory applierSingletons;
    private final InputFactory inputSingletons;
    private final OutputFactory outputSingletons;
    private final PublisherFactory publisherSingletons;
    private final AgentStepFactory agentStepSingletons;
    private final AppFactory appSingletons;

    public PassthroughAppFactory(final PassthroughAppConfig config) {
        this.pluginSingletons = Singletons.create(new DefaultPluginFactory(config, config, this::pluginSingletons));

        final Interceptor interceptor = interceptor(pluginSingletons);
        this.sequencerSingletons = interceptor.sequencerFactory(singletonsSupplier(
                (SequencerFactory) new PassthroughSequencerFactory(
                        config, config, this::sequencerSingletons, this::inputSingletons, this::applierSingletons, this::pluginSingletons
                ),
                Singletons::create
        ));
        this.applierSingletons = interceptor.applierFactory(singletonsSupplier(
                (ApplierFactory) new DefaultApplierFactory(config, applierConfig(), config, this::applierSingletons, this::pluginSingletons),
                Singletons::create
        ));
        this.inputSingletons = interceptor.inputFactory(singletonsSupplier(
                (InputFactory) new DefaultInputFactory(config, this::pluginSingletons),
                Singletons::create
        ));
        this.outputSingletons = interceptor.outputFactory(singletonsSupplier(
                (OutputFactory) new DefaultOutputFactory(config, config, this::pluginSingletons),
                Singletons::create
        ));
        this.publisherSingletons = interceptor.publisherFactory(singletonsSupplier(
                (PublisherFactory)new DefaultPublisherFactory(config, config, this::publisherSingletons,
                        this::outputSingletons, this::pluginSingletons),
                Singletons::create
        ));
        this.agentStepSingletons = interceptor.agentStepFactory(singletonsSupplier(
                (AgentStepFactory)new DefaultAgentStepFactory(config, this::pluginSingletons),
                Singletons::create
        ));
        this.appSingletons = interceptor.appFactory(singletonsSupplier(
                appFactory(), Singletons::create
        ));
    }

    private static Interceptor interceptor(final PluginFactory pluginSingletons) {
        final BaseState.Mutable baseState = pluginSingletons.baseState();
        Interceptor interceptor = Interceptor.NOOP;
        for (final Plugin.Configuration pluginConfig : pluginSingletons.plugins()) {
            interceptor = interceptor.andThen(pluginConfig.interceptor(baseState));
        }
        return interceptor.thenYield();
    }

    private ApplierConfig applierConfig() {
        final EventIdApplier eventIdApplier = (sourceId, sourceSeq, eventType, eventSeq, index) -> pluginSingletons().baseState().applyEvent(sourceId, sourceSeq, eventSeq, index);
        return () -> eventIdApplier;
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

    private PluginFactory pluginSingletons() {
        return pluginSingletons;
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
