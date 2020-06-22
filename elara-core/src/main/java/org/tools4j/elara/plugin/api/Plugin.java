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
package org.tools4j.elara.plugin.api;

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.base.BaseState.Mutable;
import org.tools4j.nobark.loop.Step;

import java.util.function.Consumer;

/**
 * API implemented by an elara plugin.
 * @param <P> the plugin state type
 */
public interface Plugin<P> {

    Input[] NO_INPUTS = {};
    Plugin.Dependency<?>[] NO_DEPENDENCIES = {};
    Consumer<Object> STATE_UNAWARE = state -> {};

    P defaultPluginState();
    Configuration configuration(org.tools4j.elara.init.Configuration appConfig, P pluginState);

    default Dependency<?>[] dependencies() {
        return NO_DEPENDENCIES;
    }

    interface Configuration {
        Step step(BaseState baseState, boolean alwaysExecute);
        Input[] inputs(BaseState baseState);
        Output output(BaseState baseState);
        CommandProcessor commandProcessor(BaseState baseState);
        EventApplier eventApplier(BaseState.Mutable baseState);

        interface Default extends Configuration {
            @Override
            default Step step(final BaseState baseState, final boolean alwaysExecute) {return Step.NO_OP;}
            @Override
            default Input[] inputs(final BaseState baseState) {return NO_INPUTS;}
            @Override
            default Output output(final BaseState baseState) {return Output.NOOP;}
            @Override
            default CommandProcessor commandProcessor(final BaseState baseState) {return CommandProcessor.NOOP;}
            @Override
            default EventApplier eventApplier(final Mutable baseState) {return EventApplier.NOOP;}
        }
    }

    @FunctionalInterface
    interface Dependency<P> {
        Plugin<P> plugin();
        default Consumer<? super P> pluginStateAware() {
            return STATE_UNAWARE;
        }
    }

    enum NullState {
        NULL;
    }
}
