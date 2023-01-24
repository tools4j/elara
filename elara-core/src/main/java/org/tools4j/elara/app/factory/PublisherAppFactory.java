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
import org.tools4j.elara.agent.PublisherAgent;
import org.tools4j.elara.app.config.InOutConfig;
import org.tools4j.elara.app.type.PublisherAppConfig;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class PublisherAppFactory implements AppFactory {

    private final PluginFactory pluginSingletons;
    private final InOutFactory inOutSingletons;
    private final PublisherFactory publisherSingletons;
    private final AppFactory appSingletons;

    public PublisherAppFactory(final PublisherAppConfig config) {
        this.pluginSingletons = Singletons.create(new DefaultPluginFactory(config, config, this::pluginSingletons));

        final Interceptor interceptor = interceptor(pluginSingletons);
        this.inOutSingletons = interceptor.inOutFactory(singletonsSupplier(
                (InOutFactory) new DefaultInOutFactory(config, inOutConfig(config), this::pluginSingletons),
                Singletons::create
        ));
        this.publisherSingletons = interceptor.publisherFactory(singletonsSupplier(
                (PublisherFactory)new StreamPublisherFactory(config, config, this::publisherSingletons, this::inOutSingletons, this::pluginSingletons),
                Singletons::create
        ));
        this.appSingletons = interceptor.appFactory(singletonsSupplier(
                appFactory(), Singletons::create
        ));
    }

    //FIXME don't use InOut if input is not supported
    private static InOutConfig inOutConfig(final PublisherAppConfig config) {
        requireNonNull(config);
        return new InOutConfig() {
            @Override
            public List<Input> inputs() {
                return Collections.emptyList();
            }

            @Override
            public Output output() {
                return config.output();
            }
        };
    }

    private static Interceptor interceptor(final PluginFactory pluginSingletons) {
        final BaseState.Mutable baseState = pluginSingletons.baseState();
        Interceptor interceptor = Interceptor.NOOP;
        for (final Plugin.Configuration pluginConfig : pluginSingletons.plugins()) {
            interceptor = interceptor.andThen(pluginConfig.interceptor(baseState));
        }
        return interceptor.thenYield();
    }

    private <T> Supplier<T> singletonsSupplier(final T factory, final UnaryOperator<T> singletonOp) {
        final T singletons = singletonOp.apply(factory);
        return () -> singletons;
    }

    private InOutFactory inOutSingletons() {
        return inOutSingletons;
    }

    private PublisherFactory publisherSingletons() {
        return publisherSingletons;
    }

    private PluginFactory pluginSingletons() {
        return pluginSingletons;
    }

    private AppFactory appFactory() {
        return () -> new PublisherAgent(publisherSingletons.publisherStep());
    }

    @Override
    public Agent agent() {
        return appSingletons.agent();
    }
}
