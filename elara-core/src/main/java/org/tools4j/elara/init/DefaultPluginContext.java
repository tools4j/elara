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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultPluginContext implements PluginContext {

    private final Context context;
    private final Map<Plugin<?>, Object> pluginStateByPlugin = new Object2ObjectHashMap<>();
    private final List<Plugin.Context> pluginContexts = new ArrayList<>();

    DefaultPluginContext(final Context context) {
        this.context = requireNonNull(context);
    }

    @Override
    public Context register(final Plugin<?> plugin) {
        pluginState(plugin);
        return context;
    }

    @Override
    public <P> Context register(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        pluginState(plugin, pluginStateProvider);
        return context;
    }

    @Override
    public <P> Context register(final Plugin<P> plugin, final Consumer<? super P> pluginStateAware) {
        final P pluginState = pluginState(plugin);
        pluginStateAware.accept(pluginState);
        return context;
    }

    @Override
    public <P> P pluginState(final Plugin<P> plugin) {
        final P existingState = pluginStateOrNull(plugin);
        if (existingState != null) {
            return existingState;
        }
        return pluginState(plugin, plugin.defaultPluginState());
    }

    @Override
    public <P> P pluginState(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        final P existingState = pluginStateOrNull(plugin);
        if (existingState != null) {
            return existingState;
        }
        return pluginState(plugin, pluginStateProvider.get());
    }

    @Override
    public <P> P pluginStateOrNull(final Plugin<P> plugin) {
        final Object state = pluginStateByPlugin.get(plugin);
        if (state != null) {
            @SuppressWarnings("unchecked")//safe because we add it safely
            final P pluginState = (P)state;
            return pluginState;
        }
        return null;
    }

    private <P> P pluginState(final Plugin<P> plugin, final P pluginState) {
        pluginStateByPlugin.put(plugin, pluginState);
        pluginContexts.add(plugin.context(pluginState));
        return pluginState;
    }

    @Override
    public List<Plugin.Context> pluginContexts() {
        return pluginContexts;
    }
}
