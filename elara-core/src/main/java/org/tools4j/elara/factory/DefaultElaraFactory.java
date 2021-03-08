/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.factory;

import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.plugin.api.Plugin;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.init.Configuration.validate;

public class DefaultElaraFactory implements ElaraFactory {

    private final Configuration configuration;
    private final Singletons singletons;

    public DefaultElaraFactory(final Configuration configuration) {
        this.configuration = validate(configuration);
        this.singletons = intercept(new DefaultSingletons(configuration, this::singletons));
    }

    private static Singletons intercept(final Singletons singletons) {
        Singletons interceptedOrNot = requireNonNull(singletons);
        for (final Plugin.Configuration pluginConfig : singletons.plugins()) {
            final InterceptableSingletons intercepted = pluginConfig.interceptOrNull(interceptedOrNot);
            if (intercepted != null) {
                if (intercepted.singletons() != interceptedOrNot) {
                    throw new IllegalStateException("Illegal interception through plugin " + pluginConfig);
                }
                interceptedOrNot = intercepted;
            }
        }
        return interceptedOrNot;
    }

    @Override
    public Configuration configuration() {
        return configuration;
    }

    @Override
    public Singletons singletons() {
        return singletons;
    }

}
