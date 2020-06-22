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

import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;
import static org.tools4j.elara.init.Configuration.validate;

public class DefaultSingletons implements Singletons {

    private final RunnerFactory runnerFactory;
    private final InputFactory inputFactory;
    private final ProcessorFactory processorFactory;
    private final ApplierFactory applierFactory;
    private final OutputFactory outputFactory;
    private final PluginFactory pluginFactory;

    private final Map<Object, Map<Class<?>, Object>> singletonsForFactory = new Object2ObjectHashMap<>(16, DEFAULT_LOAD_FACTOR);

    public DefaultSingletons(final Configuration configuration) {
        this(configuration, DefaultRunnerFactory::new, DefaultInputFactory::new, DefaultProcessorFactory::new,
                DefaultApplierFactory::new, DefaultOutputFactory::new, DefaultPluginFactory::new);
    }

    public DefaultSingletons(final Configuration configuration,
                             final FactorySupplier<? extends RunnerFactory> runnerFactorySupplier,
                             final FactorySupplier<? extends InputFactory> inputFactorySupplier,
                             final FactorySupplier<? extends ProcessorFactory> processorFactorySupplier,
                             final FactorySupplier<? extends ApplierFactory> applierFactorySupplier,
                             final FactorySupplier<? extends OutputFactory> outputFactorySupplier,
                             final FactorySupplier<? extends PluginFactory> pluginFactorySupplier) {
        validate(configuration);
        this.runnerFactory = runnerFactorySupplier.supply(configuration, this);
        this.inputFactory = inputFactorySupplier.supply(configuration, this);
        this.processorFactory = processorFactorySupplier.supply(configuration, this);
        this.applierFactory = applierFactorySupplier.supply(configuration, this);
        this.outputFactory = outputFactorySupplier.supply(configuration, this);
        this.pluginFactory = pluginFactorySupplier.supply(configuration, this);
    }

    private <T,S> T getOrCreate(final Class<T> type, S source, final Function<? super S, ? extends T> factory) {
        Map<Class<?>, Object> singletons = singletonsForFactory.get(source);
        if (singletons == null) {
            singletonsForFactory.put(source, singletons = new Object2ObjectHashMap<>(16, DEFAULT_LOAD_FACTOR));
        }
        Object value = singletons.get(type);
        if (value == null) {
            singletons.put(type, value = factory.apply(source));
        }
        return type.cast(value);
    }

    //RunnerFactory

    @Override
    public Runnable initStep() {
        return getOrCreate(Runnable.class, runnerFactory, RunnerFactory::initStep);
    }

    @Override
    public LoopCondition runningCondition() {
        return getOrCreate(LoopCondition.class, runnerFactory, RunnerFactory::runningCondition);
    }

    @Override
    public Step dutyCycleStep() {
        return getOrCreate(Step.class, runnerFactory, RunnerFactory::dutyCycleStep);
    }

    @Override
    public Step[] dutyCycleWithExtraSteps() {
        return getOrCreate(Step[].class, runnerFactory, RunnerFactory::dutyCycleWithExtraSteps);
    }

    //InputFactory


    @Override
    public Receiver receiver() {
        return getOrCreate(Receiver.class, inputFactory, InputFactory::receiver);
    }

    @Override
    public Input[] inputs() {
        return getOrCreate(Input[].class, inputFactory, InputFactory::inputs);
    }

    @Override
    public Step sequencerStep() {
        return getOrCreate(Step.class, inputFactory, InputFactory::sequencerStep);
    }

    //ProcessorFactory

    @Override
    public CommandProcessor commandProcessor() {
        return getOrCreate(CommandProcessor.class, processorFactory, ProcessorFactory::commandProcessor);
    }

    @Override
    public CommandHandler commandHandler() {
        return getOrCreate(CommandHandler.class, processorFactory, ProcessorFactory::commandHandler);
    }

    @Override
    public Step commandPollerStep() {
        return getOrCreate(Step.class, processorFactory, ProcessorFactory::commandPollerStep);
    }

    //ApplierFactory

    @Override
    public EventApplier eventApplier() {
        return getOrCreate(EventApplier.class, applierFactory, ApplierFactory::eventApplier);
    }

    @Override
    public EventHandler eventHandler() {
        return getOrCreate(EventHandler.class, applierFactory, ApplierFactory::eventHandler);
    }

    @Override
    public Step eventApplierStep() {
        return getOrCreate(Step.class, applierFactory, ApplierFactory::eventApplierStep);
    }

    //OutputFactory

    @Override
    public Output output() {
        return getOrCreate(Output.class, outputFactory, OutputFactory::output);
    }

    @Override
    public SequenceGenerator loopbackSequenceGenerator() {
        return getOrCreate(SequenceGenerator.class, outputFactory, OutputFactory::loopbackSequenceGenerator);
    }

    @Override
    public CommandLoopback commandLoopback() {
        return getOrCreate(CommandLoopback.class, outputFactory, OutputFactory::commandLoopback);
    }

    @Override
    public OutputHandler outputHandler() {
        return getOrCreate(OutputHandler.class, outputFactory, OutputFactory::outputHandler);
    }

    @Override
    public Step outputStep() {
        return getOrCreate(Step.class, outputFactory, OutputFactory::outputStep);
    }

    //PluginFactory
    @Override
    public Mutable baseState() {
        return getOrCreate(Mutable.class, pluginFactory, PluginFactory::baseState);
    }

    @Override
    public Plugin.Configuration[] plugins() {
        return getOrCreate(Plugin.Configuration[].class, pluginFactory, PluginFactory::plugins);
    }
}
