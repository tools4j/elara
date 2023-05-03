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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.PluginStateProvider;
import org.tools4j.elara.plugin.api.PluginStateProvider.NullState;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin.SystemPluginSpecification;

import static java.util.Objects.requireNonNull;

final class BootPluginSpecification implements SystemPluginSpecification<NullState> {
    private final BootPlugin bootPlugin;

    public BootPluginSpecification(final BootPlugin bootPlugin) {
        this.bootPlugin = requireNonNull(bootPlugin);
    }

    @Override
    public PluginStateProvider<NullState> defaultPluginStateProvider() {
        return PluginStateProvider.NULL_STATE_PROVIDER;
    }

    @Override
    public ReservedPayloadType reservedPayloadType() {
        return ReservedPayloadType.BOOT;
    }

    @Override
    public Installer installer(final AppConfig appConfig, final NullState pluginState) {
        requireNonNull(appConfig);
        requireNonNull(pluginState);
        return new Installer.Default() {
            @Override
            public Input input(final BaseState baseState) {
                return Input.single(bootPlugin.sourceId(), new BootCommandInput(bootPlugin, baseState));
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new BootCommandProcessor();
            }

            @Override
            public EventApplier eventApplier(final MutableBaseState baseState) {
                return new BootEventApplier(bootPlugin);
            }
        };
    }
}
