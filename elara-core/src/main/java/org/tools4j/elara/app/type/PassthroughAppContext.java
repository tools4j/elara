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
package org.tools4j.elara.app.type;

import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.app.config.AppContext;
import org.tools4j.elara.app.config.EventStoreContext;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.InputContext;
import org.tools4j.elara.app.config.OutputContext;
import org.tools4j.elara.app.config.PluginContext;
import org.tools4j.elara.app.state.BaseStateProvider;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.MultiSourceInput;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.input.UniSourceInput;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PassthroughAppContext extends PassthroughAppConfig, AppContext, EventStoreContext, InputContext,
        OutputContext, PluginContext {
    @Override
    PassthroughAppContext baseStateProvider(BaseStateProvider baseStateFactory);
    @Override
    PassthroughAppContext input(Input input);
    @Override
    PassthroughAppContext input(MultiSourceInput input);
    @Override
    PassthroughAppContext input(int sourceId, UniSourceInput input);
    @Override
    PassthroughAppContext input(int sourceId, SingleSourceInput input);
    @Override
    PassthroughAppContext output(Output output);
    @Override
    PassthroughAppContext eventStore(MessageStore eventStore);
    @Override
    PassthroughAppContext timeSource(TimeSource timeSource);

    @Override
    PassthroughAppContext exceptionHandler(ExceptionHandler exceptionHandler);
    @Override
    PassthroughAppContext duplicateHandler(DuplicateHandler duplicateHandler);
    @Override
    PassthroughAppContext loggerFactory(Logger.Factory loggerFactory);
    @Override
    PassthroughAppContext idleStrategy(IdleStrategy idleStrategy);
    @Override
    PassthroughAppContext dutyCycleExtraStep(AgentStep step, ExecutionType executionType);

    @Override
    PassthroughAppContext plugin(Plugin<?> plugin);
    @Override
    <P> PassthroughAppContext plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    @Override
    <P> PassthroughAppContext plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    @Override
    <P> PassthroughAppContext plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    PassthroughAppContext populateDefaults();
    PassthroughAppContext populateDefaults(PassthroughApp app);

    static PassthroughAppContext create() {
        return new PassthroughAppContextImpl();
    }
}
