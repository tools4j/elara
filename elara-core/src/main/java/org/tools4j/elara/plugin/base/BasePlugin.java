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
package org.tools4j.elara.plugin.base;

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.Plugin;
import org.tools4j.elara.plugin.base.BaseState.Mutable;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Default plugin to initialise {@link BaseState}.  Another plugin can be used to initialise the base state if it
 * returns an implementation of {@link BaseContext}.
 */
public final class BasePlugin implements Plugin<BaseState.Mutable> {

    @Override
    public Mutable defaultPluginState() {
        return BaseContext.createDefaultBaseStae();
    }

    @Override
    public BaseContext create(final BaseState.Mutable baseState) {
        requireNonNull(baseState);
        return () -> baseState;
    }

    @Override
    public <A> Builder<A> builder(final Function<? super A, ? extends BaseState.Mutable> stateProvider) {
        requireNonNull(stateProvider);
        return application -> create(stateProvider.apply(application));
    }

    /**
     * Base context to initialise base state.  Other plugins can implement this
     * context if they want to replace the default base plugin and extend the base
     * state.
     */
    @FunctionalInterface
    public interface BaseContext extends Context {
        static BaseState.Mutable createDefaultBaseStae() {
            return new DefaultBaseState();
        }

        BaseState.Mutable baseState();

        @Override
        default Input[] inputs(final BaseState baseState, final TimeSource timeSource, final SequenceGenerator adminSequenceGenerator) {
            return Input.EMPTY_INPUTS;
        }

        @Override
        default Output output(final BaseState baseState) {
            return Output.NOOP;
        }

        @Override
        default CommandProcessor commandProcessor(final BaseState baseState) {
            return CommandProcessor.NOOP;
        }

        @Override
        default EventApplier eventApplier(final BaseState.Mutable baseState) {
            return EventApplier.NOOP;
        }
    }
}
