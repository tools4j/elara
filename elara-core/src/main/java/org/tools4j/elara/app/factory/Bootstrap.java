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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.PluginConfig;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;

import static java.util.Objects.requireNonNull;

interface Bootstrap {
    MutableBaseState baseState();
    Installer[] plugins();
    Interceptor interceptor();

    static Bootstrap bootstrap(final AppConfig appConfig, final PluginConfig pluginConfig) {
        final Installer[] plugins = pluginConfig.plugins().toArray(new Installer[0]);
        final BootstrapStateFactory interceptorStateFactory = new BootstrapStateFactory();
        Interceptor interceptor = Interceptor.NOOP;
        for (final Installer plugin : plugins) {
            interceptor = Interceptors.concat(interceptor, plugin.interceptor(interceptorStateFactory));
        }
        interceptor = Interceptors.conclude(interceptor);
        return interceptorStateFactory.conclude(interceptor, appConfig, plugins);
    }

    class BootstrapStateFactory implements StateFactory {
        MutableBaseState baseState;
        @Override
        public MutableBaseState baseState() {
            if (baseState != null) {
                return baseState;
            }
            throw new IllegalStateException("Base state is not available during bootstrap: StateFactory.baseState() can only be called after completion of Plugin.interceptor(..) invocation.");
        }

        Bootstrap conclude(final Interceptor interceptor, final AppConfig appConfig, final Installer[] plugins) {
            requireNonNull(interceptor);
            requireNonNull(appConfig);
            requireNonNull(plugins);
            baseState = requireNonNull(interceptor.stateFactory(() -> appConfig::baseState).baseState());
            return new Bootstrap() {
                @Override
                public MutableBaseState baseState() {
                    return baseState;
                }
                @Override
                public Installer[] plugins() {
                    return plugins;
                }
                @Override
                public Interceptor interceptor() {
                    return interceptor;
                }
            };
        }
    }
}
