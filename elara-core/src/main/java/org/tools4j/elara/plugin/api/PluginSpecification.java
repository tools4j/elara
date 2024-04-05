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
package org.tools4j.elara.plugin.api;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.PluginConfigurator;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.step.AgentStep;

/**
 * The plugin specification defines the plugin's behaviour through {@link #defaultPluginStateProvider()} and
 * {@link #installer(AppConfig, Object) installer(..)}.
 *
 * @param <P> the plugin state type
 */
public interface PluginSpecification<P> {
    /**
     * Returns the default plugin state provider used to create the state for the plugin.  The specification's provider
     * is only used if no explicit provider is specified through one of the {@code plugin(..)} methods in
     * {@link PluginConfigurator} when configuring the Elara application.
     *
     * @return the default provider used to create the plugin's state
     */
    PluginStateProvider<P> defaultPluginStateProvider();
    /**
     * Returns the installer that adds plugin specific elements to the Elara application on startup.
     *
     * @param appConfig the Elara application configuration
     * @param pluginState the plugin state
     * @return the installer used at application startup to install elements necessary for the plugin to function
     */
    Installer installer(AppConfig appConfig, P pluginState);

    /**
     * Returns other plugins that the current plugin depends on.  The default implementation returns
     * {@link PluginDependency#NO_DEPENDENCIES NO_DEPENDENCIES}.
     *
     * @return the plugin dependencies of the current plugin, none by default.
     */
    default PluginDependency<?>[] dependencies() {
        return PluginDependency.NO_DEPENDENCIES;
    }

    interface Installer {
        Installer NOOP_INSTALLER = new Default() {};
        AgentStep step(BaseState baseState, ExecutionType executionType);
        Input input(BaseState baseState);
        Output output(BaseState baseState);
        CommandProcessor commandProcessor(BaseState baseState);
        EventApplier eventApplier(MutableBaseState baseState);
        Interceptor interceptor(StateFactory stateFactory);
        interface Default extends Installer {
            @Override
            default AgentStep step(final BaseState baseState, final ExecutionType executionType) {return AgentStep.NOOP;}
            @Override
            default Input input(final BaseState baseState) {return Input.NOOP;}
            @Override
            default Output output(final BaseState baseState) {return Output.NOOP;}
            @Override
            default CommandProcessor commandProcessor(final BaseState baseState) {return CommandProcessor.NOOP;}
            @Override
            default EventApplier eventApplier(final MutableBaseState baseState) {return EventApplier.NOOP;}
            @Override
            default Interceptor interceptor(final StateFactory stateFactory) {return Interceptor.NOOP;}
        }
    }

}
