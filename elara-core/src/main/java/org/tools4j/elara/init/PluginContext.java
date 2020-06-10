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

import org.agrona.collections.Object2ObjectHashMap;
import org.tools4j.elara.plugin.api.Plugin;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

final class PluginContext {

    private static final Consumer<Object> STATE_UNAWARE = state -> {};
    private static final PluginBuilder<?> DEFAULT_BUILDER = defaultBuilder();

    private final Map<Plugin<?>, PluginBuilder<?>> pluginBuilders = new Object2ObjectHashMap<>();
    private final Map<Plugin<?>, Consumer<?>> pluginStateAwares = new Object2ObjectHashMap<>();

    private interface PluginBuilder<P> {
        Plugin.Configuration build(Plugin<P> plugin, Configuration appConfig, Consumer<? super P> pluginStateAware);
    }

    void register(final Plugin<?> plugin) {
        register(plugin, STATE_UNAWARE);
    }

    <P> void register(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        register(plugin, pluginStateProvider, STATE_UNAWARE);
    }

    <P> void register(final Plugin<P> plugin, final Consumer<? super P> pluginStateAware) {
        register(plugin, defaultBuilder(), pluginStateAware);
    }

    <P> void register(final Plugin<P> plugin,
                      final Supplier<? extends P> pluginStateProvider,
                      final Consumer<? super P> pluginStateAware) {
        register(plugin, builder(plugin, pluginStateProvider), pluginStateAware);
    }

    private <P> void register(final Plugin<P> plugin,
                              final PluginBuilder<P> builder,
                              final Consumer<? super P> pluginStateAware) {
        final PluginBuilder<?> curBuilder = pluginBuilders.get(plugin);
        if (curBuilder == null || curBuilder == DEFAULT_BUILDER) {
            pluginBuilders.put(plugin, builder);
        } else {
            if (builder != curBuilder) {
                throw new IllegalStateException("plugin " + plugin + " is already registered with a different state provider");
            }
        }
        if (pluginStateAware != STATE_UNAWARE) {
            @SuppressWarnings("unchecked")//safe because we know what we added can consume <P>
            final Consumer<P> consumer = (Consumer<P>)pluginStateAwares.getOrDefault(plugin, STATE_UNAWARE);
            pluginStateAwares.put(plugin, consumer.andThen(pluginStateAware));
        }
    }

    private static <P> Plugin.Configuration build(final Plugin<P> plugin,
                                                  final Configuration appConfig,
                                                  final Consumer<? super P> pluginStateAware) {
        final P pluginState = plugin.defaultPluginState();
        pluginStateAware.accept(pluginState);
        return plugin.configuration(appConfig, pluginState);
    }

    private static <P> PluginBuilder<P> defaultBuilder() {
        final PluginBuilder<P> builder = PluginContext::build;
        assert builder == DEFAULT_BUILDER || DEFAULT_BUILDER == null;
        return builder;
    }

    private static <P> PluginBuilder<P> builder(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        requireNonNull(plugin);
        requireNonNull(pluginStateProvider);
        return (p,c,a) -> {
            assert p == plugin;
            final P pluginState = pluginStateProvider.get();
            a.accept(pluginState);
            return plugin.configuration(c, pluginState);
        };
    }

    private <P> Plugin.Configuration configuration(final Plugin<P> plugin,
                                                   final Configuration appConfig,
                                                   final PluginBuilder<?> builder) {
        @SuppressWarnings("unchecked")//safe because register method taking a builder enforces this
        final PluginBuilder<P> pluginBuilder = (PluginBuilder<P>)builder;
        @SuppressWarnings("unchecked")//safe because register method taking a consumer enforces this
        final Consumer<? super P> pluginStateAware = (Consumer<? super P>)pluginStateAwares.getOrDefault(plugin, STATE_UNAWARE);
        return pluginBuilder.build(plugin, appConfig, pluginStateAware);
    }


    List<Plugin.Configuration> configurations(final Configuration appConfig) {
        requireNonNull(appConfig);
        return pluginBuilders.entrySet().stream()
                .map(e -> configuration(e.getKey(), appConfig, e.getValue()))
                .collect(Collectors.toList());
    }
}
