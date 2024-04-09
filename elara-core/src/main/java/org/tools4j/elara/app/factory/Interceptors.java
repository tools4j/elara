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
package org.tools4j.elara.app.factory;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

enum Interceptors {
    ;
    static Interceptor concat(final Interceptor first, final Interceptor next) {
        requireNonNull(first);
        requireNonNull(next);
        if (first == Interceptor.NOOP) {
            return next;
        }
        if (next == Interceptor.NOOP) {
            return first;
        }
        return new Interceptor() {
            private <T> Supplier<? extends T> singletonOrIntercepted(final Supplier<? extends T> originalSingletons,
                                                                     final T intercepted,
                                                                     final UnaryOperator<T> singletonOp) {
                if (intercepted == null) {
                    return originalSingletons;
                }
                final T singletons = singletonOp.apply(intercepted);
                return () -> singletons;
            }

            @Override
            public AppFactory appFactory(final Supplier<? extends AppFactory> singletons) {
                return next.appFactory(singletonOrIntercepted(singletons, first.appFactory(singletons), Singletons::create));
            }

            @Override
            public StateFactory stateFactory(final Supplier<? extends StateFactory> singletons) {
                return next.stateFactory(singletonOrIntercepted(singletons, first.stateFactory(singletons), Singletons::create));
            }

            @Override
            public AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> singletons) {
                return next.agentStepFactory(singletonOrIntercepted(singletons, first.agentStepFactory(singletons), Singletons::create));
            }

            @Override
            public SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
                return next.sequencerFactory(singletonOrIntercepted(singletons, first.sequencerFactory(singletons), Singletons::create));
            }

            @Override
            public CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
                return next.commandPollerFactory(singletonOrIntercepted(singletons, first.commandPollerFactory(singletons), Singletons::create));
            }

            @Override
            public CommandProcessorFactory commandProcessorFactory(final Supplier<? extends CommandProcessorFactory> singletons) {
                return next.commandProcessorFactory(singletonOrIntercepted(singletons, first.commandProcessorFactory(singletons), Singletons::create));
            }

            @Override
            public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
                return next.applierFactory(singletonOrIntercepted(singletons, first.applierFactory(singletons), Singletons::create));
            }

            @Override
            public InputFactory inputFactory(final Supplier<? extends InputFactory> singletons) {
                return next.inputFactory(singletonOrIntercepted(singletons, first.inputFactory(singletons), Singletons::create));
            }

            @Override
            public OutputFactory outputFactory(final Supplier<? extends OutputFactory> singletons) {
                return next.outputFactory(singletonOrIntercepted(singletons, first.outputFactory(singletons), Singletons::create));
            }

            @Override
            public PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
                return next.publisherFactory(singletonOrIntercepted(singletons, first.publisherFactory(singletons), Singletons::create));
            }

            @Override
            public CommandSenderFactory commandSenderFactory(final Supplier<? extends CommandSenderFactory> singletons) {
                return next.commandSenderFactory(singletonOrIntercepted(singletons, first.commandSenderFactory(singletons), Singletons::create));
            }

            @Override
            public EventSubscriberFactory eventSubscriberFactory(final Supplier<? extends EventSubscriberFactory> singletons) {
                return next.eventSubscriberFactory(singletonOrIntercepted(singletons, first.eventSubscriberFactory(singletons), Singletons::create));
            }

            @Override
            public EventProcessorFactory eventProcessorFactory(final Supplier<? extends EventProcessorFactory> singletons) {
                return next.eventProcessorFactory(singletonOrIntercepted(singletons, first.eventProcessorFactory(singletons), Singletons::create));
            }
        };
    }

    static Interceptor conclude(final Interceptor interceptor) {
        return concat(interceptor, Yield.INSTANCE);
    }

    private enum Yield implements Interceptor {
        INSTANCE;

        @Override
        public AppFactory appFactory(final Supplier<? extends AppFactory> singletons) {
            return singletons.get();
        }

        @Override
        public StateFactory stateFactory(final Supplier<? extends StateFactory> singletons) {
            return singletons.get();
        }

        @Override
        public AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> singletons) {
            return singletons.get();
        }

        @Override
        public SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
            return singletons.get();
        }

        @Override
        public CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
            return singletons.get();
        }

        @Override
        public CommandProcessorFactory commandProcessorFactory(final Supplier<? extends CommandProcessorFactory> singletons) {
            return singletons.get();
        }

        @Override
        public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
            return singletons.get();
        }

        @Override
        public InputFactory inputFactory(final Supplier<? extends InputFactory> singletons) {
            return singletons.get();
        }

        @Override
        public OutputFactory outputFactory(final Supplier<? extends OutputFactory> singletons) {
            return singletons.get();
        }

        @Override
        public PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
            return singletons.get();
        }

        @Override
        public CommandSenderFactory commandSenderFactory(final Supplier<? extends CommandSenderFactory> singletons) {
            return singletons.get();
        }

        @Override
        public EventSubscriberFactory eventSubscriberFactory(final Supplier<? extends EventSubscriberFactory> singletons) {
            return singletons.get();
        }

        @Override
        public EventProcessorFactory eventProcessorFactory(final Supplier<? extends EventProcessorFactory> singletons) {
            return singletons.get();
        }
    }
}
