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
import org.tools4j.elara.app.config.ApplierConfigurator;
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.config.CommandProcessorConfigurator;
import org.tools4j.elara.app.config.CommandStoreConfigurator;
import org.tools4j.elara.app.config.EventStoreConfigurator;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.InputConfigurator;
import org.tools4j.elara.app.config.OutputConfigurator;
import org.tools4j.elara.app.config.PluginConfigurator;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
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

public interface AllInOneAppConfigurator extends AllInOneAppConfig, AppConfigurator, CommandStoreConfigurator,
        EventStoreConfigurator, CommandProcessorConfigurator, ApplierConfigurator, InputConfigurator, OutputConfigurator,
        PluginConfigurator {
    @Override
    AllInOneAppConfigurator baseStateProvider(BaseStateProvider baseStateFactory);
    @Override
    AllInOneAppConfigurator input(Input input);
    @Override
    AllInOneAppConfigurator input(MultiSourceInput input);
    @Override
    AllInOneAppConfigurator input(SingleSourceInput input);
    @Override
    AllInOneAppConfigurator input(int sourceId, InputPoller inputPoller);
    @Override
    AllInOneAppConfigurator output(Output output);
    @Override
    AllInOneAppConfigurator commandProcessor(CommandProcessor commandProcessor);
    @Override
    AllInOneAppConfigurator eventApplier(EventApplier eventApplier);
    @Override
    AllInOneAppConfigurator commandPollingMode(CommandPollingMode mode);
    @Override
    AllInOneAppConfigurator commandStore(MessageStore commandStore);
    @Override
    AllInOneAppConfigurator eventStore(MessageStore eventStore);
    @Override
    AllInOneAppConfigurator timeSource(TimeSource timeSource);

    @Override
    AllInOneAppConfigurator exceptionHandler(ExceptionHandler exceptionHandler);
    @Override
    AllInOneAppConfigurator duplicateHandler(DuplicateHandler duplicateHandler);
    @Override
    AllInOneAppConfigurator loggerFactory(Logger.Factory loggerFactory);
    @Override
    AllInOneAppConfigurator idleStrategy(IdleStrategy idleStrategy);
    @Override
    AllInOneAppConfigurator dutyCycleExtraStep(AgentStep step, ExecutionType executionType);

    @Override
    AllInOneAppConfigurator plugin(Plugin<?> plugin);
    @Override
    <P> AllInOneAppConfigurator plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    @Override
    <P> AllInOneAppConfigurator plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    @Override
    <P> AllInOneAppConfigurator plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    AllInOneAppConfigurator populateDefaults();
    AllInOneAppConfigurator populateDefaults(AllInOneApp app);

    static AllInOneAppConfigurator create() {
        return new AllInOneAppConfiguratorImpl();
    }
}
