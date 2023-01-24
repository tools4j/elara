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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.PluginConfig;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.base.BasePlugin.BaseConfiguration;
import org.tools4j.elara.plugin.base.BaseState;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultPluginFactory implements PluginFactory {

    private static final Plugin.Configuration[] EMPTY_PLUGIN_CONFIGURATIONS = {};

    private final AppConfig appConfig;
    private final PluginConfig pluginConfig;
    private final Supplier<? extends PluginFactory> pluginSingletons;

    public DefaultPluginFactory(final AppConfig appConfig,
                                final PluginConfig pluginConfig,
                                final Supplier<? extends PluginFactory> pluginSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.pluginConfig = requireNonNull(pluginConfig);
        this.pluginSingletons = requireNonNull(pluginSingletons);
    }

    @Override
    public Plugin.Configuration[] plugins() {
        final List<Plugin.Configuration> plugins = pluginConfig.plugins();
        boolean basePluginFound = false;
        for (int i = 0; i < plugins.size(); i++) {
            basePluginFound |= plugins.get(i) instanceof BaseConfiguration;
        }
        if (basePluginFound) {
            return pluginConfig.plugins().toArray(EMPTY_PLUGIN_CONFIGURATIONS);
        }
        final Plugin.Configuration[] pluginsWithBasePlugin = new Plugin.Configuration[plugins.size() + 1];
        plugins.toArray(pluginsWithBasePlugin);
        pluginsWithBasePlugin[pluginsWithBasePlugin.length - 1] = Plugins.basePlugin().configuration(
                appConfig, BaseConfiguration.createDefaultBaseState(appConfig)
        );
        return pluginsWithBasePlugin;
    }

    @Override
    public BaseState.Mutable baseState() {
        for (final Plugin.Configuration plugin : pluginSingletons.get().plugins()) {
            if (plugin instanceof BaseConfiguration) {
                return ((BaseConfiguration)plugin).baseState();
            }
        }
        throw new IllegalStateException("Plugins must contain BaseConfiguration instance");
    }
}
