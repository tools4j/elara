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

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.app.config.AppConfigurator;
import org.tools4j.elara.app.config.DefaultPluginConfigurator;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.config.PluginConfigurator;
import org.tools4j.elara.app.state.BaseStateProvider;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.exception.ExceptionLogger;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.input.MultiSourceInput;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.logging.Logger.Factory;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;
import org.tools4j.elara.plugin.boot.BootCommandInputPoller;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.time.TimeSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.logging.OutputStreamLogger.SYSTEM_FACTORY;

abstract class AbstractAppConfigurator<T extends AbstractAppConfigurator<T>> implements AppConfigurator, PluginConfigurator {

    private BaseStateProvider baseStateProvider;
    private Input input = Input.NOOP;
    private Output output = Output.NOOP;
    private TimeSource timeSource;
    private ExceptionHandler exceptionHandler = ExceptionHandler.systemDefault();
    private Factory loggerFactory = SYSTEM_FACTORY;
    private DuplicateHandler duplicateHandler = DuplicateHandler.systemDefault();
    private IdleStrategy idleStrategy = new BackoffIdleStrategy(
            100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
    private final EnumMap<ExecutionType, List<AgentStep>> extraSteps = new EnumMap<>(ExecutionType.class);
    private final DefaultPluginConfigurator plugins = new DefaultPluginConfigurator(this);

    abstract protected T self();

    @Override
    public MutableBaseState baseState() {
        return (baseStateProvider != null ? baseStateProvider : BaseStateProvider.DEFAULT).createBaseState(this);
    }

    @Override
    public T baseStateProvider(final BaseStateProvider baseStateFactory) {
        this.baseStateProvider = requireNonNull(baseStateFactory);
        return self();
    }

    public Input input() {
        return input;
    }

    public T input(final Input input) {
        return input(input, false);
    }

    private T input(final Input input, final boolean isBoot) {
        requireNonNull(input);
        this.input = this.input == Input.NOOP ? input :
                //we want boot input to be the first to be polled
                isBoot ? Input.roundRobin(input, this.input) : Input.roundRobin(this.input, input);
        return self();
    }

    public T input(final MultiSourceInput input) {
        return input(Input.multi(input), false);
    }

    public T input(final SingleSourceInput input) {
        return input(Input.single(input), false);
    }

    public T input(final int sourceId, final InputPoller inputPoller) {
        return input(Input.single(sourceId, inputPoller), inputPoller instanceof BootCommandInputPoller);
    }

    public Output output() {
        return output;
    }

    public T output(final Output output) {
        this.output = requireNonNull(output);
        return self();
    }

    @Override
    public TimeSource timeSource() {
        return timeSource;
    }

    @Override
    public T timeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;//null allowed here
        return self();
    }

    @Override
    public ExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public T exceptionHandler(final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = requireNonNull(exceptionHandler);
        return self();
    }

    public DuplicateHandler duplicateHandler() {
        return duplicateHandler;
    }

    public T duplicateHandler(final DuplicateHandler duplicateHandler) {
        this.duplicateHandler = requireNonNull(duplicateHandler);
        return self();
    }

    @Override
    public Factory loggerFactory() {
        return loggerFactory;
    }

    @Override
    public T loggerFactory(final Factory loggerFactory) {
        this.loggerFactory = requireNonNull(loggerFactory);
        ExceptionLogger exceptionLogger = null;
        if (duplicateHandler == DuplicateHandler.systemDefault() && loggerFactory != SYSTEM_FACTORY) {
            duplicateHandler = exceptionLogger = new ExceptionLogger(loggerFactory, true);
        }
        if (exceptionHandler == ExceptionHandler.systemDefault() && loggerFactory != SYSTEM_FACTORY) {
            exceptionHandler = exceptionLogger != null ? exceptionLogger : new ExceptionLogger(loggerFactory, true);
        }
        return self();
    }

    @Override
    public IdleStrategy idleStrategy() {
        return idleStrategy;
    }

    @Override
    public T idleStrategy(final IdleStrategy idleStrategy) {
        this.idleStrategy = requireNonNull(idleStrategy);
        return self();
    }

    @Override
    public List<AgentStep> dutyCycleExtraSteps(final ExecutionType executionType) {
        return extraSteps.getOrDefault(executionType, Collections.emptyList());
    }

    @Override
    public T dutyCycleExtraStep(final AgentStep step, final ExecutionType executionType) {
        requireNonNull(step);
        requireNonNull(executionType);
        extraSteps.computeIfAbsent(executionType, k -> new ArrayList<>()).add(step);
        return self();
    }

    @Override
    public T plugin(final Plugin<?> plugin) {
        plugins.plugin(plugin);
        return self();
    }

    @Override
    public <P> T plugin(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider) {
        plugins.plugin(plugin, pluginStateProvider);
        return self();
    }

    @Override
    public <P> T plugin(final Plugin<P> plugin, final Consumer<? super P> pluginStateAware) {
        plugins.plugin(plugin, pluginStateAware);
        return self();
    }

    @Override
    public <P> T plugin(final Plugin<P> plugin, final Supplier<? extends P> pluginStateProvider, final Consumer<? super P> pluginStateAware) {
        plugins.plugin(plugin, pluginStateProvider, pluginStateAware);
        return self();
    }

    @Override
    public List<Installer> plugins() {
        return plugins.plugins();
    }

    protected T populateDefaults() {
        if (timeSource == null) {
            timeSource = System::currentTimeMillis;
        }
        return self();
    }

    @Override
    public void validate() {
        if (timeSource() == null) {
            throw new IllegalArgumentException("Time source must be set");
        }
    }
}
