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
import org.tools4j.elara.agent.PublisherAgent;
import org.tools4j.elara.app.type.PublisherAppConfig;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static org.tools4j.elara.app.factory.Bootstrap.bootstrap;

public class PublisherAppFactory implements AppFactory {
    private final OutputFactory outputSingletons;
    private final PublisherFactory publisherSingletons;
    private final AppFactory appSingletons;

    public PublisherAppFactory(final PublisherAppConfig config) {
        final Bootstrap bootstrap = bootstrap(config, config);
        final Interceptor interceptor = bootstrap.interceptor();
        this.outputSingletons = interceptor.outputFactory(singletonsSupplier(
                (OutputFactory) new DefaultOutputFactory(config, config, bootstrap.baseState(), bootstrap.plugins()),
                Singletons::create
        ));
        this.publisherSingletons = interceptor.publisherFactory(singletonsSupplier(
                (PublisherFactory)new StreamPublisherFactory(config, config, this::publisherSingletons, this::outputSingletons),
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

    private OutputFactory outputSingletons() {
        return outputSingletons;
    }

    private PublisherFactory publisherSingletons() {
        return publisherSingletons;
    }

    private AppFactory appFactory() {
        return () -> new PublisherAgent(publisherSingletons.publisherStep());
    }

    @Override
    public Agent agent() {
        return appSingletons.agent();
    }
}
