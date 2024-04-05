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
import org.tools4j.elara.app.config.EventStreamConfigurator;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.OutputConfigurator;
import org.tools4j.elara.app.config.PluginConfigurator;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.state.BaseStateProvider;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Poller;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface PublisherAppConfigurator extends PublisherAppConfig, AppConfigurator, EventStreamConfigurator, OutputConfigurator,
        PluginConfigurator {

    @Override
    PublisherAppConfigurator baseStateProvider(BaseStateProvider baseStateFactory);
    @Override
    PublisherAppConfigurator eventStore(MessageStore eventStore);
    @Override
    PublisherAppConfigurator eventStore(Poller eventStorePoller);
    @Override
    PublisherAppConfigurator eventReceiver(MessageReceiver eventReceiver);
    @Override
    PublisherAppConfigurator eventProcessor(EventProcessor eventProcessor);

    @Override
    PublisherAppConfigurator output(Output output);
    @Override
    PublisherAppConfigurator timeSource(TimeSource timeSource);

    @Override
    PublisherAppConfigurator exceptionHandler(ExceptionHandler exceptionHandler);
    @Override
    PublisherAppConfigurator loggerFactory(Logger.Factory loggerFactory);
    @Override
    PublisherAppConfigurator idleStrategy(IdleStrategy idleStrategy);
    @Override
    PublisherAppConfigurator dutyCycleExtraStep(AgentStep step, ExecutionType executionType);

    @Override
    PublisherAppConfigurator plugin(Plugin<?> plugin);
    @Override
    <P> PublisherAppConfigurator plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    @Override
    <P> PublisherAppConfigurator plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    @Override
    <P> PublisherAppConfigurator plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    PublisherAppConfigurator populateDefaults();
    PublisherAppConfigurator populateDefaults(PublisherApp app);

    static PublisherAppConfigurator create() {
        return new PublisherAppConfiguratorImpl();
    }
}
