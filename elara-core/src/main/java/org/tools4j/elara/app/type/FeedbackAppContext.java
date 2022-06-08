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
import org.tools4j.elara.app.config.AppContext;
import org.tools4j.elara.app.config.CommandStreamContext;
import org.tools4j.elara.app.config.EventStreamContext;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.PluginContext;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Poller;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.MessageStream;
import org.tools4j.elara.time.TimeSource;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface FeedbackAppContext extends FeedbackAppConfig, AppContext, EventStreamContext, CommandStreamContext, PluginContext {

    @Override
    FeedbackAppContext eventStream(MessageStore eventStore);
    @Override
    FeedbackAppContext eventStream(Poller eventStorePoller);
    @Override
    FeedbackAppContext eventStream(MessageStream eventStream);
    @Override
    FeedbackAppContext eventProcessor(EventProcessor eventProcessor);
    @Override
    FeedbackAppContext commandStream(MessageStore commandStore);
    @Override
    FeedbackAppContext commandStream(MessageSender commandSender);

    @Override
    FeedbackAppContext input(Input input);
    @Override
    FeedbackAppContext inputs(Input... inputs);
    @Override
    FeedbackAppContext inputs(Collection<? extends Input> inputs);
    @Override
    FeedbackAppContext output(Output output);

    @Override
    FeedbackAppContext timeSource(TimeSource timeSource);
    @Override
    FeedbackAppContext exceptionHandler(ExceptionHandler exceptionHandler);
    @Override
    FeedbackAppContext duplicateHandler(DuplicateHandler duplicateHandler);
    @Override
    FeedbackAppContext loggerFactory(Logger.Factory loggerFactory);
    @Override
    FeedbackAppContext idleStrategy(IdleStrategy idleStrategy);
    @Override
    FeedbackAppContext dutyCycleExtraStep(AgentStep step, ExecutionType executionType);

    @Override
    FeedbackAppContext plugin(Plugin<?> plugin);
    @Override
    <P> FeedbackAppContext plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider);
    @Override
    <P> FeedbackAppContext plugin(Plugin<P> plugin, Consumer<? super P> pluginStateAware);
    @Override
    <P> FeedbackAppContext plugin(Plugin<P> plugin, Supplier<? extends P> pluginStateProvider, Consumer<? super P> pluginStateAware);

    FeedbackAppContext populateDefaults();
    FeedbackAppContext populateDefaults(FeedbackApp app);

    static FeedbackAppContext create() {
        return new FeedbackAppContextImpl();
    }
}
