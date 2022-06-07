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
package org.tools4j.elara.app.factory;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public interface Interceptor {

    Interceptor NOOP = new Interceptor() {};

    default AppFactory appFactory(final Supplier<? extends AppFactory> singletons) {
        return null;
    }

    default AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> singletons) {
        return null;
    }

    default SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
        return null;
    }

    default CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
        return null;
    }

    default ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
        return null;
    }

    default ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
        return null;
    }

    default InOutFactory inOutFactory(final Supplier<? extends InOutFactory> singletons) {
        return null;
    }

    default PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
        return null;
    }

    default CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> singletons) {
        return null;
    }

    default EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> singletons) {
        return null;
    }

    default Interceptor andThen(final Interceptor next) {
        requireNonNull(next);
        if (this == NOOP) {
            return next;
        }
        if (next == NOOP) {
            return this;
        }
        final Interceptor first = this;
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
            public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
                return next.processorFactory(singletonOrIntercepted(singletons, first.processorFactory(singletons), Singletons::create));
            }

            @Override
            public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
                return next.applierFactory(singletonOrIntercepted(singletons, first.applierFactory(singletons), Singletons::create));
            }

            @Override
            public InOutFactory inOutFactory(final Supplier<? extends InOutFactory> singletons) {
                return next.inOutFactory(singletonOrIntercepted(singletons, first.inOutFactory(singletons), Singletons::create));
            }

            @Override
            public PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
                return next.publisherFactory(singletonOrIntercepted(singletons, first.publisherFactory(singletons), Singletons::create));
            }

            @Override
            public CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> singletons) {
                return next.commandStreamFactory(singletonOrIntercepted(singletons, first.commandStreamFactory(singletons), Singletons::create));
            }

            @Override
            public EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> singletons) {
                return next.eventStreamFactory(singletonOrIntercepted(singletons, first.eventStreamFactory(singletons), Singletons::create));
            }
        };
    }

    default Interceptor thenYield() {
        return andThen(Yield.INSTANCE);
    }

    enum Yield implements Interceptor {
        INSTANCE;

        @Override
        public final AppFactory appFactory(final Supplier<? extends AppFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final InOutFactory inOutFactory(final Supplier<? extends InOutFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> singletons) {
            return singletons.get();
        }

        @Override
        public final Interceptor andThen(final Interceptor next) {
            throw new IllegalStateException("Cannot intercept after yield");
        }

        @Override
        public final Interceptor thenYield() {
            return this;
        }
    }
}
