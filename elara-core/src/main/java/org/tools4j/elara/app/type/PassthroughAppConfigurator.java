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
package org.tools4j.elara.app.type;

import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.app.config.AppConfigurator;
import org.tools4j.elara.app.config.EventStoreConfigurator;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.InputConfigurator;
import org.tools4j.elara.app.config.OutputConfigurator;
import org.tools4j.elara.app.config.PluginConfigurator;
import org.tools4j.elara.app.state.BaseStateProvider;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.input.MultiSourceInput;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PassthroughAppConfigurator extends PassthroughAppConfig, AppConfigurator, EventStoreConfigurator, InputConfigurator,
        OutputConfigurator, PluginConfigurator {
    @Override
    PassthroughAppConfigurator baseStateProvider(BaseStateProvider baseStateFactory);
    @Override
    PassthroughAppConfigurator input(Input input);
    @Override
    PassthroughAppConfigurator input(MultiSourceInput input);
    @Override
    PassthroughAppConfigurator input(SingleSourceInput input);
    @Override
    PassthroughAppConfigurator input(int sourceId, InputPoller inputPoller);
    @Override
    PassthroughAppConfigurator output(Output output);
    @Override
    PassthroughAppConfigurator eventStore(MessageStore eventStore);
    @Override
    PassthroughAppConfigurator timeSource(TimeSource timeSource);

    @Override
    PassthroughAppConfigurator exceptionHandler(ExceptionHandler exceptionHandler);
    @Override
    PassthroughAppConfigurator duplicateHandler(DuplicateHandler duplicateHandler);
    @Override
    PassthroughAppConfigurator loggerFactory(Logger.Factory loggerFactory);
    @Override
    PassthroughAppConfigurator idleStrategy(IdleStrategy idleStrategy);
    @Override
    PassthroughAppConfigurator dutyCycleExtraStep(AgentStep step, ExecutionType executionType);

    @Override
    PassthroughAppConfigurator plugin(Plugin<?> plugin);
    @Override
    <P> PassthroughAppConfigurator plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    @Override
    <P> PassthroughAppConfigurator plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    @Override
    <P> PassthroughAppConfigurator plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    PassthroughAppConfigurator populateDefaults();
    PassthroughAppConfigurator populateDefaults(PassthroughApp app);

    static PassthroughAppConfigurator create() {
        return new PassthroughAppConfiguratorImpl();
    }
}
