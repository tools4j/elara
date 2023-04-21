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
package org.tools4j.elara.plugin.api;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.step.AgentStep;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * API implemented by an elara plugin.
 * @param <P> the plugin state type
 */
public interface Plugin<P> {

    Plugin.Dependency<?>[] NO_DEPENDENCIES = {};
    Consumer<Object> STATE_UNAWARE = state -> {};

    P defaultPluginState(AppConfig appConfig);
    Configuration configuration(AppConfig appConfig, P pluginState);

    default Dependency<?>[] dependencies() {
        return NO_DEPENDENCIES;
    }

    interface Configuration {
        AgentStep step(BaseState baseState, ExecutionType executionType);
        Input input(BaseState baseState);
        Output output(BaseState baseState);
        CommandProcessor commandProcessor(BaseState baseState);
        EventApplier eventApplier(BaseState.Mutable baseState);
        Interceptor interceptor(BaseState.Mutable baseState);

        interface Default extends Configuration {
            @Override
            default AgentStep step(final BaseState baseState, final ExecutionType executionType) {return AgentStep.NOOP;}
            @Override
            default Input input(final BaseState baseState) {return Input.NOOP;}
            @Override
            default Output output(final BaseState baseState) {return Output.NOOP;}
            @Override
            default CommandProcessor commandProcessor(final BaseState baseState) {return CommandProcessor.NOOP;}
            @Override
            default EventApplier eventApplier(final BaseState.Mutable baseState) {return EventApplier.NOOP;}
            @Override
            default Interceptor interceptor(final BaseState.Mutable baseState) {return Interceptor.NOOP;}
        }
    }

    @FunctionalInterface
    interface Dependency<P> {
        Plugin<P> plugin();
        default Consumer<? super P> pluginStateAware() {
            return STATE_UNAWARE;
        }

        static <P> Dependency<P> of(final Plugin<P> plugin) {
            requireNonNull(plugin);
            return () -> plugin;
        }

        static <P> Dependency<P> of(final Plugin<P> plugin, final Consumer<? super P> pluginStateAware) {
            requireNonNull(plugin);
            requireNonNull(pluginStateAware);
            return new Dependency<P>() {
                @Override
                public Plugin<P> plugin() {
                    return plugin;
                }

                @Override
                public Consumer<? super P> pluginStateAware() {
                    return pluginStateAware;
                }
            };
        }
    }

    enum NullState {
        NULL
    }
}
