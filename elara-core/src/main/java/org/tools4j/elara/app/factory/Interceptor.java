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

    default AppFactory appFactory(final Supplier<? extends AppFactory> original) {
        return null;
    }

    default AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> original) {
        return null;
    }

    default SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> original) {
        return null;
    }

    default CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> original) {
        return null;
    }

    default ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> original) {
        return null;
    }

    default ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> original) {
        return null;
    }

    default InOutFactory inOutFactory(final Supplier<? extends InOutFactory> original) {
        return null;
    }

    default PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> original) {
        return null;
    }

    default CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> original) {
        return null;
    }

    default EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> original) {
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
            private <T> Supplier<? extends T> originalOrIntercepted(final Supplier<? extends T> original,
                                                                    final T intercepted,
                                                                    final UnaryOperator<T> singletonOp) {
                if (intercepted == null) {
                    return original;
                }
                final T singleton = singletonOp.apply(intercepted);
                return () -> singleton;
            }

            @Override
            public AppFactory appFactory(final Supplier<? extends AppFactory> original) {
                return next.appFactory(originalOrIntercepted(original, first.appFactory(original), Singletons::create));
            }

            @Override
            public AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> original) {
                return next.agentStepFactory(originalOrIntercepted(original, first.agentStepFactory(original), Singletons::create));
            }

            @Override
            public SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> original) {
                return next.sequencerFactory(originalOrIntercepted(original, first.sequencerFactory(original), Singletons::create));
            }

            @Override
            public CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> original) {
                return next.commandPollerFactory(originalOrIntercepted(original, first.commandPollerFactory(original), Singletons::create));
            }

            @Override
            public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> original) {
                return next.processorFactory(originalOrIntercepted(original, first.processorFactory(original), Singletons::create));
            }

            @Override
            public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> original) {
                return next.applierFactory(originalOrIntercepted(original, first.applierFactory(original), Singletons::create));
            }

            @Override
            public InOutFactory inOutFactory(final Supplier<? extends InOutFactory> original) {
                return next.inOutFactory(originalOrIntercepted(original, first.inOutFactory(original), Singletons::create));
            }

            @Override
            public PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> original) {
                return next.publisherFactory(originalOrIntercepted(original, first.publisherFactory(original), Singletons::create));
            }

            @Override
            public CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> original) {
                return next.commandStreamFactory(originalOrIntercepted(original, first.commandStreamFactory(original), Singletons::create));
            }

            @Override
            public EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> original) {
                return next.eventStreamFactory(originalOrIntercepted(original, first.eventStreamFactory(original), Singletons::create));
            }
        };
    }

    default Interceptor thenYield() {
        return andThen(Yield.INSTANCE);
    }

    enum Yield implements Interceptor {
        INSTANCE;

        @Override
        public final AppFactory appFactory(final Supplier<? extends AppFactory> original) {
            return original.get();
        }

        @Override
        public final AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> original) {
            return original.get();
        }

        @Override
        public final SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> original) {
            return original.get();
        }

        @Override
        public final CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> original) {
            return original.get();
        }

        @Override
        public final ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> original) {
            return original.get();
        }

        @Override
        public final ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> original) {
            return original.get();
        }

        @Override
        public final InOutFactory inOutFactory(final Supplier<? extends InOutFactory> original) {
            return original.get();
        }

        @Override
        public final PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> original) {
            return original.get();
        }

        @Override
        public final CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> original) {
            return original.get();
        }

        @Override
        public final EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> original) {
            return original.get();
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
