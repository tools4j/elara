package org.tools4j.elara.app.factory;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.plugin.api.Plugin;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

enum Interceptors {
    ;
    static InterceptorInitializer interceptor(final PluginFactory pluginSingletons) {
        final InterceptorStateFactory interceptorStateFactory = new InterceptorStateFactory();
        Interceptor interceptor = Interceptor.NOOP;
        for (final Plugin.Configuration pluginConfig : pluginSingletons.plugins()) {
            interceptor = concat(interceptor, pluginConfig.interceptor(interceptorStateFactory));
        }
        interceptor = concat(interceptor, Yield.INSTANCE);
        return interceptorStateFactory.resolve(interceptor);
    }

    interface InterceptorInitialized {
        BaseState baseState();
        Interceptor interceptor();
    }

    interface InterceptorInitializer {
        InterceptorInitialized initialize(AppConfig appConfig);
    }

    enum UnavailableStateFactory implements StateFactory {
        INSTANCE;

        @Override
        public MutableBaseState baseState() {
            throw new IllegalStateException("StateFactor.baseState() cannot be called before Plugin.interceptor(..) invocation has competed.");
        }
    }

    private static class InterceptorStateFactory implements StateFactory {
        MutableBaseState baseState;
        @Override
        public MutableBaseState baseState() {
            return baseState != null ? baseState : UnavailableStateFactory.INSTANCE.baseState();
        }

        InterceptorInitializer resolve(final Interceptor interceptor) {
            requireNonNull(interceptor);
            return appConfig -> {
                baseState = requireNonNull(interceptor.stateFactory(() -> appConfig::baseState).baseState());
                return new InterceptorInitialized() {
                    @Override
                    public BaseState baseState() {
                        return baseState;
                    }

                    @Override
                    public Interceptor interceptor() {
                        return interceptor;
                    }
                };
            };
        }
    }

    private static Interceptor concat(final Interceptor first, final Interceptor next) {
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
            public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
                return next.processorFactory(singletonOrIntercepted(singletons, first.processorFactory(singletons), Singletons::create));
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
            public CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> singletons) {
                return next.commandStreamFactory(singletonOrIntercepted(singletons, first.commandStreamFactory(singletons), Singletons::create));
            }

            @Override
            public EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> singletons) {
                return next.eventStreamFactory(singletonOrIntercepted(singletons, first.eventStreamFactory(singletons), Singletons::create));
            }
        };
    }

    enum Yield implements Interceptor {
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
        public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
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
        public CommandStreamFactory commandStreamFactory(final Supplier<? extends CommandStreamFactory> singletons) {
            return singletons.get();
        }

        @Override
        public EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> singletons) {
            return singletons.get();
        }
    }
}
