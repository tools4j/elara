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
package org.tools4j.elara.app.config;

import org.agrona.concurrent.Agent;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.type.AllInOneApp;
import org.tools4j.elara.app.type.AllInOneAppContext;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.factory.ElaraFactory;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.logging.Logger.Factory;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.time.TimeSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.logging.OutputStreamLogger.SYSTEM_FACTORY;

@Deprecated
final class DefaultContext implements Context {

    private static final String DEFAULT_THREAD_NAME = "elara";

    private CommandProcessor commandProcessor;
    private EventApplier eventApplier;
    private final List<Input> inputs = new ArrayList<>();
    private Output output = Output.NOOP;
    private MessageStore commandStore;
    private CommandPollingMode commandStreamMode = CommandPollingMode.FROM_END;
    private MessageStore eventStore;
    private TimeSource timeSource;
    private ExceptionHandler exceptionHandler = ExceptionHandler.DEFAULT;
    private Logger.Factory loggerFactory = SYSTEM_FACTORY;
    private DuplicateHandler duplicateHandler = DuplicateHandler.DEFAULT;
    private IdleStrategy idleStrategy = new BackoffIdleStrategy(
            100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
    private final EnumMap<ExecutionType, List<AgentStep>> extraSteps = new EnumMap<>(ExecutionType.class);
    private ThreadFactory threadFactory;
    private final DefaultPluginContext plugins = new DefaultPluginContext(this);

    @Override
    public CommandProcessor commandProcessor() {
        return commandProcessor;
    }

    @Override
    public Context commandProcessor(final CommandProcessor commandProcessor) {
        this.commandProcessor = requireNonNull(commandProcessor);
        return this;
    }

    @Override
    public EventApplier eventApplier() {
        return eventApplier;
    }

    @Override
    public Context eventApplier(final EventApplier eventApplier) {
        this.eventApplier = requireNonNull(eventApplier);
        return this;
    }

    @Override
    public List<Input> inputs() {
        return inputs;
    }

    @Override
    public Context input(final Input input) {
        inputs.add(input);
        return this;
    }

    @Override
    public Context inputs(final Input... inputs) {
        for (final Input input : inputs) {
            input(input);
        }
        return this;
    }

    @Override
    public Context inputs(final Collection<? extends Input> inputs) {
        this.inputs.addAll(inputs);
        return this;
    }

    @Override
    public Output output() {
        return output;
    }

    @Override
    public Context output(final Output output) {
        this.output = requireNonNull(output);
        return this;
    }

    @Override
    public MessageStore commandStore() {
        return commandStore;
    }

    @Override
    public Context commandStore(final MessageStore commandStore) {
        this.commandStore = requireNonNull(commandStore);
        return this;
    }

    @Override
    public CommandPollingMode commandPollingMode() {
        return commandStreamMode;
    }

    @Override
    public Context commandPollingMode(final CommandPollingMode mode) {
        this.commandStreamMode = requireNonNull(mode);
        return this;
    }

    @Override
    public MessageStore eventStore() {
        return eventStore;
    }

    @Override
    public Context eventStore(final String file) {
        throw new IllegalStateException("not supported yet");
    }

    @Override
    public Context eventStore(final MessageStore eventStore) {
        this.eventStore = requireNonNull(eventStore);
        return this;
    }

    @Override
    public TimeSource timeSource() {
        return timeSource;
    }

    @Override
    public Context timeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;//null allowed here
        return this;
    }

    @Override
    public ExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public Context exceptionHandler(final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = requireNonNull(exceptionHandler);
        return this;
    }

    @Override
    public DuplicateHandler duplicateHandler() {
        return duplicateHandler;
    }

    @Override
    public Context duplicateHandler(final DuplicateHandler duplicateHandler) {
        this.duplicateHandler = requireNonNull(duplicateHandler);
        return this;
    }

    @Override
    public Factory loggerFactory() {
        return loggerFactory;
    }

    @Override
    public Context loggerFactory(final Factory loggerFactory) {
        this.loggerFactory = requireNonNull(loggerFactory);
        if (duplicateHandler == DuplicateHandler.DEFAULT && loggerFactory != SYSTEM_FACTORY) {
            duplicateHandler = DuplicateHandler.loggingHandler(loggerFactory);
        }
        return this;
    }

    @Override
    public IdleStrategy idleStrategy() {
        return idleStrategy;
    }

    @Override
    public Context idleStrategy(final IdleStrategy idleStrategy) {
        this.idleStrategy = requireNonNull(idleStrategy);
        return this;
    }

    @Override
    public List<AgentStep> dutyCycleExtraSteps(final ExecutionType executionType) {
        return extraSteps.getOrDefault(executionType, Collections.emptyList());
    }

    @Override
    public Context dutyCycleExtraStep(final AgentStep step, final ExecutionType executionType) {
        requireNonNull(step);
        requireNonNull(executionType);
        extraSteps.computeIfAbsent(executionType, k -> new ArrayList<>()).add(step);
        return this;
    }

    @Override
    public ThreadFactory threadFactory() {
        return threadFactory;
    }

    @Override
    public Context threadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;//null allowed here
        return this;
    }

    @Override
    public Context threadFactory(final String threadName) {
        return threadFactory(threadName == null ? null : r -> new Thread(null, r, threadName));
    }

    @Override
    public Context plugin(final Plugin<?> plugin) {
        plugins.plugin(plugin);
        return this;
    }

    @Override
    public <P> Context plugin(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        plugins.plugin(plugin, pluginStateProvider);
        return this;
    }

    @Override
    public <P> Context plugin(final Plugin<P> plugin, final Consumer<? super P> pluginStateAware) {
        plugins.plugin(plugin, pluginStateAware);
        return this;
    }

    @Override
    public <P> Context plugin(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider, final Consumer<? super P> pluginStateAware) {
        plugins.plugin(plugin, pluginStateProvider, pluginStateAware);
        return this;
    }

    @Override
    public List<Plugin.Configuration> plugins() {
        return plugins.configurations(this);
    }

    @Override
    public Context populateDefaults() {
        if (timeSource == null) {
            timeSource = System::currentTimeMillis;
        }
        if (threadFactory == null) {
            threadFactory(DEFAULT_THREAD_NAME);
        }
        return this;
    }

    @Override
    public AllInOneAppContext populateDefaults(final AllInOneApp app) {
        return this
                .commandProcessor(app)
                .eventApplier(app)
                .output(app)
                .populateDefaults();
    }

    @Override
    public void validate() {
        if (commandProcessor() == null) {
            throw new IllegalArgumentException("Command processor must be set");
        }
        if (eventApplier() == null) {
            throw new IllegalArgumentException("Event applier must be set");
        }
        if (commandStore() == null && commandPollingMode() != CommandPollingMode.NO_STORE) {
            throw new IllegalArgumentException("Command log must be set unless command polling mode is NO_STORE");
        }
        if (eventStore() == null) {
            throw new IllegalArgumentException("Event log must be set");
        }
        if (timeSource() == null) {
            throw new IllegalArgumentException("Time source must be set");
        }
        if (threadFactory() == null) {
            throw new IllegalArgumentException("Thread factory must be set");
        }
    }

    @Override
    public Agent createAgent() {
        populateDefaults().validate();
        return ElaraFactory.create(this).singletons().agent();
    }
}
