/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.factory;

import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.Agent;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.source.SourceContextProvider;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.stream.MessageStream;

import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

final class Singletons {

    private final Map<String, Object> instanceByName = new Object2ObjectHashMap<>(64, DEFAULT_LOAD_FACTOR);

    <T,S> T getOrCreate(final String name,
                        final Class<T> type,
                        final S factoryInstance,
                        final Function<? super S, ? extends T> factoryMethod) {
        Object value = instanceByName.get(name);
        if (value == null) {
            instanceByName.put(name, value = factoryMethod.apply(factoryInstance));
        }
        return type.cast(value);
    }

    static StateFactory create(final StateFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new StateFactory() {
            @Override
            public MutableBaseState baseState() {
                return singletons.getOrCreate("baseState", MutableBaseState.class, factory, StateFactory::baseState);
            }
        };
    }

    static AppFactory create(final AppFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new AppFactory() {
            @Override
            public Agent agent() {
                return singletons.getOrCreate("agent", Agent.class, factory, AppFactory::agent);
            }
        };
    }

    static SequencerFactory create(final SequencerFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        return new SequencerFactory() {
            @Override
            public SourceContextProvider sourceContextProvider() {
                return singletons.getOrCreate("sourceContextProvider", SourceContextProvider.class, factory, SequencerFactory::sourceContextProvider);
            }

            @Override
            public SenderSupplier senderSupplier() {
                return singletons.getOrCreate("senderSupplier", SenderSupplier.class, factory, SequencerFactory::senderSupplier);
            }

            @Override
            public AgentStep sequencerStep() {
                return singletons.getOrCreate("sequencerStep", AgentStep.class, factory, SequencerFactory::sequencerStep);
            }
        };
    }

    static ProcessorFactory create(final ProcessorFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        return new ProcessorFactory() {
            @Override
            public CommandProcessor commandProcessor() {
                return singletons.getOrCreate("commandProcessor", CommandProcessor.class, factory, ProcessorFactory::commandProcessor);
            }

            @Override
            public CommandHandler commandHandler() {
                return singletons.getOrCreate("commandHandler", CommandHandler.class, factory, ProcessorFactory::commandHandler);
            }
        };
    }

    static ApplierFactory create(final ApplierFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        return new ApplierFactory() {
            @Override
            public EventApplier eventApplier() {
                return singletons.getOrCreate("eventApplier", EventApplier.class, factory, ApplierFactory::eventApplier);
            }

            @Override
            public EventHandler eventHandler() {
                return singletons.getOrCreate("eventHandler", EventHandler.class, factory, ApplierFactory::eventHandler);
            }

            @Override
            public AgentStep eventPollerStep() {
                return singletons.getOrCreate("eventPollerStep", AgentStep.class, factory, ApplierFactory::eventPollerStep);
            }
        };
    }

    static CommandPollerFactory create(final CommandPollerFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new CommandPollerFactory() {
            @Override
            public AgentStep commandPollerStep() {
                return singletons.getOrCreate("commandPollerStep", AgentStep.class, factory, CommandPollerFactory::commandPollerStep);
            }
        };
    }

    static CommandStreamFactory create(final CommandStreamFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new CommandStreamFactory() {
            @Override
            public SourceContextProvider sourceContextProvider() {
                return singletons.getOrCreate("sourceContextProvider", SourceContextProvider.class, factory, CommandStreamFactory::sourceContextProvider);
            }

            @Override
            public AgentStep inputPollerStep() {
                return singletons.getOrCreate("inputPollerStep", AgentStep.class, factory, CommandStreamFactory::inputPollerStep);
            }
        };
    }

    static EventStreamFactory create(final EventStreamFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new EventStreamFactory() {
            @Override
            public MessageStream eventStream() {
                return singletons.getOrCreate("eventStream", MessageStream.class, factory, EventStreamFactory::eventStream);
            }

            @Override
            public EventProcessor eventProcessor() {
                return singletons.getOrCreate("eventProcessor", EventProcessor.class, factory, EventStreamFactory::eventProcessor);
            }

            @Override
            public Output output() {
                return singletons.getOrCreate("output", Output.class, factory, EventStreamFactory::output);
            }
        };
    }

    static AgentStepFactory create(final AgentStepFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        return new AgentStepFactory() {
            @Override
            public AgentStep extraStepAlwaysWhenEventsApplied() {
                return singletons.getOrCreate("extraStepAlwaysWhenEventsApplied", AgentStep.class, factory, AgentStepFactory::extraStepAlwaysWhenEventsApplied);
            }

            @Override
            public AgentStep extraStepAlways() {
                return singletons.getOrCreate("extraStepAlways", AgentStep.class, factory, AgentStepFactory::extraStepAlways);
            }
        };
    }

    static InputFactory create(final InputFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new InputFactory() {
            @Override
            public Input input() {
                return singletons.getOrCreate("input", Input.class, factory, InputFactory::input);
            }
        };
    }

    static OutputFactory create(final OutputFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        //noinspection Convert2Lambda
        return new OutputFactory() {
            @Override
            public Output output() {
                return singletons.getOrCreate("output", Output.class, factory, OutputFactory::output);
            }
        };
    }

    static PublisherFactory create(final PublisherFactory factory) {
        requireNonNull(factory);
        final Singletons singletons = new Singletons();
        return new PublisherFactory() {
            @Override
            public OutputHandler outputHandler() {
                return singletons.getOrCreate("outputHandler", OutputHandler.class, factory, PublisherFactory::outputHandler);
            }

            @Override
            public AgentStep publisherStep() {
                return singletons.getOrCreate("publisherStep", AgentStep.class, factory, PublisherFactory::publisherStep);
            }
        };
    }
}
