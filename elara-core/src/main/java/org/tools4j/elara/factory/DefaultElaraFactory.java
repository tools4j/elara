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
import org.tools4j.elara.input.ReceiverFactory;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.loop.DutyCycleStep;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState.Mutable;
import org.tools4j.elara.time.TimeSource;
import org.tools4j.nobark.loop.Step;

import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

public class DefaultElaraFactory implements ElaraFactory {

    private final Configuration configuration;
    private final InputFactory inputFactory;
    private final ProcessorFactory processorFactory;
    private final ApplierFactory applierFactory;
    private final OutputFactory outputFactory;
    private final PluginFactory pluginFactory;
    private final Singletons singletons = new Singletons();

    public DefaultElaraFactory(final Configuration configuration) {
        this(configuration, DefaultInputFactory::new, DefaultProcessorFactory::new, DefaultApplierFactory::new,
                DefaultOutputFactory::new, DefaultPluginFactory::new);
    }

    public DefaultElaraFactory(final Configuration configuration,
                               final Function<? super ElaraFactory, ? extends InputFactory> inputFactorySupplier,
                               final Function<? super ElaraFactory, ? extends ProcessorFactory> processorFactorySupplier,
                               final Function<? super ElaraFactory, ? extends ApplierFactory> applierFactorySupplier,
                               final Function<? super ElaraFactory, ? extends OutputFactory> outputFactorySupplier,
                               final Function<? super ElaraFactory, ? extends PluginFactory> pluginFactorySupplier) {
        this.configuration = requireNonNull(configuration);
        this.inputFactory = inputFactorySupplier.apply(this);
        this.processorFactory = processorFactorySupplier.apply(this);
        this.applierFactory = applierFactorySupplier.apply(this);
        this.outputFactory = outputFactorySupplier.apply(this);
        this.pluginFactory = pluginFactorySupplier.apply(this);
    }

    @Override
    public Configuration configuration() {
        return configuration;
    }

    @Override
    public InputFactory inputFactory() {
        return singletons.getOrCreate(InputFactory.class, this, self -> self.new SingletonInputFactory());
    }

    @Override
    public ProcessorFactory processorFactory() {
        return singletons.getOrCreate(ProcessorFactory.class, this, self -> self.new SingletonProcessorFactory());
    }

    @Override
    public ApplierFactory applierFactory() {
        return singletons.getOrCreate(ApplierFactory.class, this, self -> self.new SingletonApplierFactory());
    }

    @Override
    public OutputFactory outputFactory() {
        return singletons.getOrCreate(OutputFactory.class, this, self -> self.new SingletonOutputFactory());
    }

    @Override
    public PluginFactory pluginFactory() {
        return singletons.getOrCreate(PluginFactory.class, this, self -> self.new SingletonPluginFactory());
    }

    @Override
    public DutyCycleStep dutyCycleStep() {
        return new DutyCycleStep(
                inputFactory().sequencerStep(),
                processorFactory().commandPollerStep(),
                applierFactory().eventApplierStep(),
                outputFactory().outputStep()
        );
    }

    private static final class Singletons {
        final Map<Object, Map<Class<?>, Object>> singletonsForSource = new Object2ObjectHashMap<>(16, DEFAULT_LOAD_FACTOR);

        <T,S> T getOrCreate(final Class<T> type, S source, Function<? super S, ? extends T> factory) {
            Map<Class<?>, Object> singletons = singletonsForSource.get(source);
            if (singletons == null) {
                singletonsForSource.put(source, singletons = new Object2ObjectHashMap<>(16, DEFAULT_LOAD_FACTOR));
            }
            Object value = singletons.get(type);
            if (value == null) {
                singletons.put(type, value = factory.apply(source));
            }
            return type.cast(value);
        }
    }

    private final class SingletonInputFactory implements InputFactory {
        @Override
        public TimeSource timeSource() {
            return singletons.getOrCreate(TimeSource.class, inputFactory, InputFactory::timeSource);
        }

        @Override
        public ReceiverFactory receiverFactory() {
            return singletons.getOrCreate(ReceiverFactory.class, inputFactory, InputFactory::receiverFactory);
        }

        @Override
        public Input[] inputs() {
            return singletons.getOrCreate(Input[].class, inputFactory, InputFactory::inputs);
        }

        @Override
        public Step sequencerStep() {
            return singletons.getOrCreate(Step.class, inputFactory, InputFactory::sequencerStep);
        }
    }

    private final class SingletonProcessorFactory implements ProcessorFactory {
        @Override
        public CommandProcessor commandProcessor() {
            return singletons.getOrCreate(CommandProcessor.class, processorFactory, ProcessorFactory::commandProcessor);
        }

        @Override
        public CommandHandler commandHandler() {
            return singletons.getOrCreate(CommandHandler.class, processorFactory, ProcessorFactory::commandHandler);
        }

        @Override
        public Step commandPollerStep() {
            return singletons.getOrCreate(Step.class, processorFactory, ProcessorFactory::commandPollerStep);
        }
    }

    private final class SingletonApplierFactory implements ApplierFactory {
        @Override
        public EventApplier eventApplier() {
            return singletons.getOrCreate(EventApplier.class, applierFactory, ApplierFactory::eventApplier);
        }

        @Override
        public EventHandler eventHandler() {
            return singletons.getOrCreate(EventHandler.class, applierFactory, ApplierFactory::eventHandler);
        }

        @Override
        public Step eventApplierStep() {
            return singletons.getOrCreate(Step.class, applierFactory, ApplierFactory::eventApplierStep);
        }
    }

    private final class SingletonOutputFactory implements OutputFactory {
        @Override
        public Output output() {
            return singletons.getOrCreate(Output.class, outputFactory, OutputFactory::output);
        }

        @Override
        public SequenceGenerator loopbackSequenceGenerator() {
            return singletons.getOrCreate(SequenceGenerator.class, outputFactory, OutputFactory::loopbackSequenceGenerator);
        }

        @Override
        public CommandLoopback commandLoopback() {
            return singletons.getOrCreate(CommandLoopback.class, outputFactory, OutputFactory::commandLoopback);
        }

        @Override
        public OutputHandler outputHandler() {
            return singletons.getOrCreate(OutputHandler.class, outputFactory, OutputFactory::outputHandler);
        }

        @Override
        public Step outputStep() {
            return singletons.getOrCreate(Step.class, outputFactory, OutputFactory::outputStep);
        }
    }

    private final class SingletonPluginFactory implements PluginFactory {

        @Override
        public Mutable baseState() {
            return singletons.getOrCreate(Mutable.class, pluginFactory, PluginFactory::baseState);
        }

        @Override
        public org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins() {
            return singletons.getOrCreate(org.tools4j.elara.plugin.api.Plugin.Configuration[].class, pluginFactory, PluginFactory::plugins);
        }
    }
}
