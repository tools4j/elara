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
package org.tools4j.elara.factory;

import org.agrona.collections.Object2ObjectHashMap;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState.Mutable;
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.loop.Step;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;
import static org.tools4j.elara.init.Configuration.validate;

public class DefaultSingletons implements Singletons {

    private final RunnerFactory runnerFactory;
    private final InputFactory inputFactory;
    private final ProcessorFactory processorFactory;
    private final ApplierFactory applierFactory;
    private final OutputFactory outputFactory;
    private final PluginFactory pluginFactory;

    private final Map<String, Object> instanceByName = new Object2ObjectHashMap<>(64, DEFAULT_LOAD_FACTOR);

    public DefaultSingletons(final Configuration configuration, final Supplier<? extends Singletons> singletonsSupplier) {
        this(configuration, singletonsSupplier, DefaultRunnerFactory::new, DefaultInputFactory::new,
                DefaultProcessorFactory::new, DefaultApplierFactory::new, DefaultOutputFactory::new,
                DefaultPluginFactory::new);
    }

    public DefaultSingletons(final Configuration configuration,
                             final Supplier<? extends Singletons> singletonsSupplier,
                             final FactorySupplier<? extends RunnerFactory> runnerFactorySupplier,
                             final FactorySupplier<? extends InputFactory> inputFactorySupplier,
                             final FactorySupplier<? extends ProcessorFactory> processorFactorySupplier,
                             final FactorySupplier<? extends ApplierFactory> applierFactorySupplier,
                             final FactorySupplier<? extends OutputFactory> outputFactorySupplier,
                             final FactorySupplier<? extends PluginFactory> pluginFactorySupplier) {
        validate(configuration);
        this.runnerFactory = runnerFactorySupplier.supply(configuration, singletonsSupplier);
        this.inputFactory = inputFactorySupplier.supply(configuration, singletonsSupplier);
        this.processorFactory = processorFactorySupplier.supply(configuration, singletonsSupplier);
        this.applierFactory = applierFactorySupplier.supply(configuration, singletonsSupplier);
        this.outputFactory = outputFactorySupplier.supply(configuration, singletonsSupplier);
        this.pluginFactory = pluginFactorySupplier.supply(configuration, singletonsSupplier);
    }

    private <T,S> T getOrCreate(final String name, final Class<T> type, final S source, final Function<? super S, ? extends T> factory) {
        Object value = instanceByName.get(name);
        if (value == null) {
            instanceByName.put(name, value = factory.apply(source));
        }
        return type.cast(value);
    }

    //RunnerFactory

    @Override
    public Runnable initStep() {
        return getOrCreate("initStep", Runnable.class, runnerFactory, RunnerFactory::initStep);
    }

    @Override
    public LoopCondition runningCondition() {
        return getOrCreate("runningCondition", LoopCondition.class, runnerFactory, RunnerFactory::runningCondition);
    }

    @Override
    public Step dutyCycleStep() {
        return getOrCreate("dutyCycleStep", Step.class, runnerFactory, RunnerFactory::dutyCycleStep);
    }

    @Override
    public Step dutyCycleExtraStep() {
        return getOrCreate("dutyCycleExtraStep", Step.class, runnerFactory, RunnerFactory::dutyCycleExtraStep);
    }

    @Override
    public Step[] dutyCycleWithExtraSteps() {
        return getOrCreate("dutyCycleWithExtraSteps", Step[].class, runnerFactory, RunnerFactory::dutyCycleWithExtraSteps);
    }

    //InputFactory

    @Override
    public Receiver receiver() {
        return getOrCreate("receiver", Receiver.class, inputFactory, InputFactory::receiver);
    }

    @Override
    public Input[] inputs() {
        return getOrCreate("inputs", Input[].class, inputFactory, InputFactory::inputs);
    }

    @Override
    public Step sequencerStep() {
        return getOrCreate("sequencerStep", Step.class, inputFactory, InputFactory::sequencerStep);
    }

    //ProcessorFactory

    @Override
    public CommandProcessor commandProcessor() {
        return getOrCreate("commandProcessor", CommandProcessor.class, processorFactory, ProcessorFactory::commandProcessor);
    }

    @Override
    public CommandHandler commandHandler() {
        return getOrCreate("commandHandler", CommandHandler.class, processorFactory, ProcessorFactory::commandHandler);
    }

    @Override
    public Step commandPollerStep() {
        return getOrCreate("commandPollerStep", Step.class, processorFactory, ProcessorFactory::commandPollerStep);
    }

    //ApplierFactory

    @Override
    public EventApplier eventApplier() {
        return getOrCreate("eventApplier", EventApplier.class, applierFactory, ApplierFactory::eventApplier);
    }

    @Override
    public EventHandler eventHandler() {
        return getOrCreate("eventHandler", EventHandler.class, applierFactory, ApplierFactory::eventHandler);
    }

    @Override
    public Step eventPollerStep() {
        return getOrCreate("eventPollerStep", Step.class, applierFactory, ApplierFactory::eventPollerStep);
    }

    //OutputFactory

    @Override
    public Output output() {
        return getOrCreate("output", Output.class, outputFactory, OutputFactory::output);
    }

    @Override
    public SequenceGenerator loopbackSequenceGenerator() {
        return getOrCreate("loopbackSequenceGenerator", SequenceGenerator.class, outputFactory, OutputFactory::loopbackSequenceGenerator);
    }

    @Override
    public CommandLoopback commandLoopback() {
        return getOrCreate("commandLoopback", CommandLoopback.class, outputFactory, OutputFactory::commandLoopback);
    }

    @Override
    public OutputHandler outputHandler() {
        return getOrCreate("outputHandler", OutputHandler.class, outputFactory, OutputFactory::outputHandler);
    }

    @Override
    public Step outputStep() {
        return getOrCreate("outputStep", Step.class, outputFactory, OutputFactory::outputStep);
    }

    //PluginFactory

    @Override
    public Mutable baseState() {
        return getOrCreate("baseState", Mutable.class, pluginFactory, PluginFactory::baseState);
    }

    @Override
    public Plugin.Configuration[] plugins() {
        return getOrCreate("plugins", Plugin.Configuration[].class, pluginFactory, PluginFactory::plugins);
    }
}
