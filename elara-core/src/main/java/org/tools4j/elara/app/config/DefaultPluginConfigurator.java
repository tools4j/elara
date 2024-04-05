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
package org.tools4j.elara.app.config;

import org.agrona.collections.Object2ObjectHashMap;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.api.PluginDependency;
import org.tools4j.elara.plugin.api.PluginSpecification;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class DefaultPluginConfigurator implements PluginConfigurator {

    private static final Consumer<Object> STATE_UNAWARE = state -> {};
    private static final InstallerProvider<?> DEFAULT_INSTALLER_PROVIDER = defaultInstallerProvider();

    private final AppConfig appConfig;
    private final Map<Plugin<?>, InstallerProvider<?>> pluginInstallerProviders = new Object2ObjectHashMap<>();
    private final Map<Plugin<?>, Consumer<?>> pluginStateAwares = new Object2ObjectHashMap<>();

    private interface InstallerProvider<P> {
        Installer install(Plugin<P> plugin, AppConfig appConfig, Consumer<? super P> pluginStateAware);
    }

    public DefaultPluginConfigurator(final AppConfig appConfig) {
        this.appConfig = requireNonNull(appConfig);
    }

    @Override
    public List<Installer> plugins() {
        return installers(appConfig);
    }

    @Override
    public PluginConfigurator plugin(final Plugin<?> plugin) {
        return plugin(plugin, STATE_UNAWARE);
    }

    @Override
    public <P> PluginConfigurator plugin(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        return plugin(plugin, pluginStateProvider, STATE_UNAWARE);
    }

    @Override
    public <P> PluginConfigurator plugin(final Plugin<P> plugin, final Consumer<? super P> pluginStateAware) {
        register(plugin, defaultInstallerProvider(), pluginStateAware);
        return this;
    }

    @Override
    public <P> PluginConfigurator plugin(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider, final Consumer<? super P> pluginStateAware) {
        register(plugin, installerProvider(plugin, pluginStateProvider), pluginStateAware);
        return this;
    }

    private <P> void register(final Plugin<P> plugin,
                              final InstallerProvider<P> installerProvider,
                              final Consumer<? super P> pluginStateAware) {
        final InstallerProvider<?> curProvider = pluginInstallerProviders.get(plugin);
        if (curProvider == null || curProvider == DEFAULT_INSTALLER_PROVIDER) {
            pluginInstallerProviders.put(plugin, installerProvider);
        } else {
            if (installerProvider != DEFAULT_INSTALLER_PROVIDER && installerProvider != curProvider) {
                throw new IllegalStateException("plugin " + plugin + " is already registered with a different state provider");
            }
        }
        if (pluginStateAware != STATE_UNAWARE) {
            @SuppressWarnings("unchecked")//safe because we know what we added can consume <P>
            final Consumer<P> consumer = (Consumer<P>) this.pluginStateAwares.getOrDefault(plugin, STATE_UNAWARE);
            this.pluginStateAwares.put(plugin, consumer.andThen(pluginStateAware));
        }
        if (curProvider == null) {
            //only register dependencies if the plugin was not already registered before
            for (final PluginDependency<?> dependency : plugin.specification().dependencies()) {
                dependency.install(this);
            }
        }
    }

    private static <P> Installer install(final Plugin<P> plugin,
                                         final AppConfig appConfig,
                                         final Consumer<? super P> pluginStateAware) {
        final PluginSpecification<P> spec = plugin.specification();
        final P pluginState = spec.defaultPluginStateProvider().createPluginState(appConfig);
        pluginStateAware.accept(pluginState);
        return spec.installer(appConfig, pluginState);
    }

    private static <P> InstallerProvider<P> defaultInstallerProvider() {
        final InstallerProvider<P> installer = DefaultPluginConfigurator::install;
        assert installer == DEFAULT_INSTALLER_PROVIDER || DEFAULT_INSTALLER_PROVIDER == null;
        return installer;
    }

    private static <P> InstallerProvider<P> installerProvider(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        requireNonNull(plugin);
        requireNonNull(pluginStateProvider);
        return (p,c,a) -> {
            assert p == plugin;
            final P pluginState = pluginStateProvider.get();
            a.accept(pluginState);
            return plugin.specification().installer(c, pluginState);
        };
    }

    private <P> Installer installer(final Plugin<P> plugin,
                                    final AppConfig appConfig,
                                    final InstallerProvider<?> provider) {
        @SuppressWarnings("unchecked")//safe because register method taking a provider enforces this
        final InstallerProvider<P> installerProvider = (InstallerProvider<P>)provider;
        @SuppressWarnings("unchecked")//safe because register method taking a consumer enforces this
        final Consumer<? super P> stateAware = (Consumer<? super P>) this.pluginStateAwares.getOrDefault(plugin, STATE_UNAWARE);
        return installerProvider.install(plugin, appConfig, stateAware);
    }


    List<Installer> installers(final AppConfig appConfig) {
        requireNonNull(appConfig);
        return pluginInstallerProviders.entrySet().stream()
                .map(e -> installer(e.getKey(), appConfig, e.getValue()))
                .collect(Collectors.toList());
    }
}
