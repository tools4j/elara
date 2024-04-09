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
import org.tools4j.elara.agent.AllInOneAgent;
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.type.AllInOneAppConfig;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.tools4j.elara.app.factory.Bootstrap.bootstrap;

public class AllInOneAppFactory implements AppFactory {
    private final SequencerFactory sequencerSingletons;
    private final CommandProcessorFactory commandProcessorSingletons;
    private final ApplierFactory applierSingletons;
    private final InputFactory inputSingletons;
    private final OutputFactory outputSingletons;
    private final CommandPollerFactory commandPollerSingletons;
    private final PublisherFactory publisherSingletons;
    private final AgentStepFactory agentStepSingletons;
    private final AppFactory appSingletons;

    public AllInOneAppFactory(final AllInOneAppConfig config) {
        final Bootstrap bootstrap = bootstrap(config, config);
        final Interceptor interceptor = bootstrap.interceptor();
        this.sequencerSingletons = interceptor.sequencerFactory(singletonsSupplier(
                config.commandPollingMode() == CommandPollingMode.NO_STORE ?
                        new ProcessingSequencerFactory(config, bootstrap.baseState(), this::sequencerSingletons, this::commandProcessorSingletons, this::inputSingletons) :
                        new AppendingSequencerFactory(config, config, bootstrap.baseState(), this::sequencerSingletons, this::inputSingletons),
                Singletons::create
        ));
        this.commandProcessorSingletons = interceptor.commandProcessorFactory(singletonsSupplier(
                (CommandProcessorFactory) new DefaultCommandProcessorFactory(config, config, config, config, bootstrap.baseState(), bootstrap.plugins(), this::commandProcessorSingletons, this::applierSingletons),
                Singletons::create
        ));
        this.applierSingletons = interceptor.applierFactory(singletonsSupplier(
                (ApplierFactory) new DefaultApplierFactory(config, config, config, bootstrap.baseState(), bootstrap.plugins(), this::applierSingletons),
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
        this.commandPollerSingletons = interceptor.commandPollerFactory(singletonsSupplier(
                (CommandPollerFactory)new DefaultCommandPollerFactory(config, this::commandPollerSingletons, this::commandProcessorSingletons),
                Singletons::create
        ));
        this.publisherSingletons = interceptor.publisherFactory(singletonsSupplier(
                (PublisherFactory)new StorePublisherFactory(config, config, bootstrap.baseState(), this::publisherSingletons, this::outputSingletons),
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

    private <T> Supplier<T> singletonsSupplier(final T factory, final UnaryOperator<T> singletonOp) {
        final T singletons = singletonOp.apply(factory);
        return () -> singletons;
    }

    private CommandPollerFactory commandPollerSingletons() {
        return commandPollerSingletons;
    }

    private CommandProcessorFactory commandProcessorSingletons() {
        return commandProcessorSingletons;
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
//        return () -> new CoreAgent(
//                sequencerSingletons.sequencerStep(),
//                commandPollerSingletons.commandPollerStep(),
//                applierSingletons.eventPollerStep(),
//                agentStepSingletons.extraStepAlwaysWhenEventsApplied(),
//                agentStepSingletons.extraStepAlways());
        return () -> new AllInOneAgent(
                sequencerSingletons.sequencerStep(),
                commandPollerSingletons.commandPollerStep(),
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
