/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.base.BasePlugin.BaseConfiguration;
import org.tools4j.elara.plugin.base.BaseState;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultPluginFactory implements PluginFactory {

    private static final Plugin.Configuration[] EMPTY_PLUGIN_CONFIGURATIONS = {};

    private final Configuration configuration;
    private final Supplier<? extends Singletons> singletons;

    public DefaultPluginFactory(final Configuration configuration, final Supplier<? extends Singletons> singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins() {
        final List<Plugin.Configuration> plugins = configuration.plugins();
        boolean basePluginFound = false;
        for (int i = 0; i < plugins.size(); i++) {
            basePluginFound |= plugins.get(i) instanceof BaseConfiguration;
        }
        if (basePluginFound) {
            return configuration.plugins().toArray(EMPTY_PLUGIN_CONFIGURATIONS);
        }
        final Plugin.Configuration[] pluginsWithBasePlugin = new Plugin.Configuration[plugins.size() + 1];
        plugins.toArray(pluginsWithBasePlugin);
        pluginsWithBasePlugin[pluginsWithBasePlugin.length - 1] = Plugins.basePlugin().configuration(
                configuration, BaseConfiguration.createDefaultBaseState()
        );
        return pluginsWithBasePlugin;
    }

    @Override
    public BaseState.Mutable baseState() {
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : singletons.get().plugins()) {
            if (plugin instanceof BaseConfiguration) {
                return ((BaseConfiguration)plugin).baseState();
            }
        }
        throw new IllegalStateException("Plugins must contain BaseConfiguration instance");
    }
}
