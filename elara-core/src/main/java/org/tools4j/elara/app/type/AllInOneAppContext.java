/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.config.CoreContext;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.InOutContext;
import org.tools4j.elara.app.config.PluginContext;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.time.TimeSource;

import java.util.Collection;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface AllInOneAppContext extends AllInOneAppConfig, CoreContext, InOutContext, PluginContext {
    @Override
    AllInOneAppContext input(Input input);
    @Override
    AllInOneAppContext inputs(Input... inputs);
    @Override
    AllInOneAppContext inputs(Collection<? extends Input> inputs);
    @Override
    AllInOneAppContext output(Output output);
    @Override
    AllInOneAppContext commandProcessor(CommandProcessor commandProcessor);
    @Override
    AllInOneAppContext eventApplier(EventApplier eventApplier);
    @Override
    AllInOneAppContext commandPollingMode(CommandPollingMode mode);
    @Override
    AllInOneAppContext commandStore(MessageStore commandStore);
    @Override
    AllInOneAppContext eventStore(MessageStore eventStore);
    @Override
    AllInOneAppContext timeSource(TimeSource timeSource);

    @Override
    AllInOneAppContext exceptionHandler(ExceptionHandler exceptionHandler);
    @Override
    AllInOneAppContext duplicateHandler(DuplicateHandler duplicateHandler);
    @Override
    AllInOneAppContext loggerFactory(Logger.Factory loggerFactory);
    @Override
    AllInOneAppContext idleStrategy(IdleStrategy idleStrategy);
    @Override
    AllInOneAppContext dutyCycleExtraStep(AgentStep step, ExecutionType executionType);
    @Override
    AllInOneAppContext threadFactory(String threadName);
    @Override
    AllInOneAppContext threadFactory(ThreadFactory threadFactory);

    @Override
    AllInOneAppContext plugin(Plugin<?> plugin);
    @Override
    <P> AllInOneAppContext plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    @Override
    <P> AllInOneAppContext plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    @Override
    <P> AllInOneAppContext plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    AllInOneAppContext populateDefaults();
    AllInOneAppContext populateDefaults(AllInOneApp app);

    static AllInOneAppContext create() {
        return new AllInOneAppContextImpl();
    }
}
