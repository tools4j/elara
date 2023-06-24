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
import org.tools4j.elara.agent.FeedbackAgent;
import org.tools4j.elara.app.type.FeedbackAppConfig;

import static org.tools4j.elara.app.factory.Bootstrap.bootstrap;

public class FeedbackAppFactory implements AppFactory {
    private final EventStreamFactory eventStreamSingletons;
    private final CommandStreamFactory commandStreamSingletons;
    private final InputFactory inputSingletons;
    private final OutputFactory outputSingletons;
    private final PublisherFactory publisherSingletons;
    private final AgentStepFactory agentStepSingletons;
    private final AppFactory appSingletons;

    public FeedbackAppFactory(final FeedbackAppConfig config) {
        final Bootstrap bootstrap = bootstrap(config, config);
        final Interceptor interceptor = bootstrap.interceptor();
        this.eventStreamSingletons = interceptor.applierFactory(Singletons.supplier(
                (EventStreamFactory) new DefaultEventStreamFactory(config, config, bootstrap.baseState(), bootstrap.plugins(),
                        this::commandStreamSingletons, this::outputSingletons, this::eventStreamSingletons),
                Singletons::create
        ));
        this.commandStreamSingletons = interceptor.applierFactory(Singletons.supplier(
                (CommandStreamFactory) new SendingCommandStreamFactory(config, config, bootstrap.baseState(),
                        this::commandStreamSingletons, this::inputSingletons),
                Singletons::create
        ));
        this.inputSingletons = interceptor.inputFactory(Singletons.supplier(
                (InputFactory) new DefaultInputFactory(config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.outputSingletons = interceptor.outputFactory(Singletons.supplier(
                (OutputFactory) new DefaultOutputFactory(config, config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.publisherSingletons = interceptor.publisherFactory(Singletons.supplier(
                (PublisherFactory)new DefaultPublisherFactory(config, config, bootstrap.baseState(), this::publisherSingletons, this::outputSingletons),
                Singletons::create
        ));
        this.agentStepSingletons = interceptor.agentStepFactory(Singletons.supplier(
                (AgentStepFactory)new DefaultAgentStepFactory(config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.appSingletons = interceptor.appFactory(Singletons.supplier(
                appFactory(), Singletons::create
        ));
    }

    private EventStreamFactory eventStreamSingletons() {
        return eventStreamSingletons;
    }
    private CommandStreamFactory commandStreamSingletons() {
        return eventStreamSingletons;
    }

    private InputFactory inputSingletons() {
        return inputSingletons;
    }

    private OutputFactory outputSingletons() {
        return outputSingletons;
    }

    private PublisherFactory publisherSingletons() {
        return publisherSingletons;
    }

    private AppFactory appFactory() {
        return () -> new FeedbackAgent(
                eventStreamSingletons.eventPollerStep(),
                inputSingletons.input().inputPollerStep(commandStreamSingletons.sourceContextProvider()),
                publisherSingletons.publisherStep(),
                agentStepSingletons.extraStepAlwaysWhenEventsApplied(),
                agentStepSingletons.extraStepAlways());
    }

    @Override
    public Agent agent() {
        return appSingletons.agent();
    }
}
