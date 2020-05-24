/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.init;

import org.tools4j.elara.plugin.Plugin;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public interface PluginConfigurer<A> {
    PluginConfigurer<A> plugin(Plugin<?> plugin);

    PluginConfigurer<A> plugin(Plugin.Builder<? super A> plugin);

    <P> PluginConfigurer<A> plugin(Plugin<P> plugin, BiConsumer<? super A, ? super P> applicationInitializer);

    <P> PluginConfigurer<A> plugin(Plugin<P> plugin, Function<? super A, ? extends P> pluginStateProvider);

    List<Plugin.Builder<? super A>> plugins();

    static <A> PluginConfigurer<A> create() {
        return new DefaultPluginConfigurer<>();
    }

    static <A> PluginConfigurer<A> create(final A application) {
        return create();
    }

    static <A> PluginConfigurer<A> create(final Class<A> applicationType) {
        return create();
    }
}
