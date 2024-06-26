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
package org.tools4j.elara.plugin.activation;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.factory.CommandPollerFactory;
import org.tools4j.elara.app.factory.CommandProcessorFactory;
import org.tools4j.elara.app.factory.CommandSenderFactory;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.SequencerFactory;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.state.MutableInFlightState;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.route.CommandTransaction;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.source.CommandSourceProvider;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Poller;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class ActivationPluginInterceptor implements Interceptor {

    private final AppConfig appConfig;
    private final ActivationPlugin plugin;
    private final StateFactory stateFactory;

    private Supplier<? extends CommandProcessorFactory> processorSingletons;

    public ActivationPluginInterceptor(final AppConfig appConfig,
                                       final ActivationPlugin plugin,
                                       final StateFactory stateFactory) {
        this.plugin = requireNonNull(plugin);
        this.appConfig = requireNonNull(appConfig);
        this.stateFactory = requireNonNull(stateFactory);
    }

    @Override
    public CommandSenderFactory commandSenderFactory(final Supplier<? extends CommandSenderFactory> singletons) {
        return new CommandSenderFactory() {
            @Override
            public MutableInFlightState inFlightState() {
                return singletons.get().inFlightState();
            }

            @Override
            public CommandContext commandContext() {
                return singletons.get().commandContext();
            }

            @Override
            public CommandSourceProvider commandSourceProvider() {
                return singletons.get().commandSourceProvider();
            }

            @Override
            public SenderSupplier senderSupplier() {
                final CommandCachingMode mode = plugin.config().commandCachingMode();
                switch (mode) {
                    case REPLAY:
                        return new CachingSenderSupplier(appConfig, plugin);
                    case REJECT://fallthrough
                    case DISCARD:
                        return new DiscardingSenderSupplier(appConfig, plugin, singletons.get().senderSupplier());
                    default:
                        throw new IllegalStateException("Invalid command replay mode in activation plugin config: " + mode);
                }
            }
        };
    }

    @Override
    public SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
        if (hasCommandStore(appConfig)) {
            return null;//no interception required in this case
        }
        return new SequencerFactory() {
            @Override
            public CommandContext commandContext() {
                return singletons.get().commandContext();
            }

            @Override
            public CommandSourceProvider commandSourceProvider() {
                return singletons.get().commandSourceProvider();
            }

            @Override
            public SenderSupplier senderSupplier() {
                final SenderSupplier activeSenderSupplier = singletons.get().senderSupplier();
                return new DiscardingSenderSupplier(appConfig, plugin, activeSenderSupplier);
            }

            @Override
            public AgentStep sequencerStep() {
                return singletons.get().sequencerStep();
            }
        };
    }

    @Override
    public CommandProcessorFactory commandProcessorFactory(final Supplier<? extends CommandProcessorFactory> singletons) {
        if (hasCommandStore(appConfig)) {
            this.processorSingletons = requireNonNull(singletons);
            return null;//no interception required in this case
        }
        return new CommandProcessorFactory() {
            @Override
            public CommandProcessor commandProcessor() {
                return singletons.get().commandProcessor();
            }

            @Override
            public CommandTransaction commandTransaction() {
                return singletons.get().commandTransaction();
            }

            @Override
            public CommandHandler commandHandler() {
                final CommandHandler handler = singletons.get().commandHandler();
                return command -> {
                    if (plugin.isActive()) {
                        handler.onCommand(command);
                    }
                };
            }
        };
    }

    @Override
    public CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
        requireNonNull(singletons);
        if (!hasCommandStore(appConfig)) {
            return null;//no interception required in this case
        }
        return new CommandPollerFactory() {
            @Override
            public Poller commandMessagePoller() {
                return singletons.get().commandMessagePoller();
            }
            @Override
            public Handler commandMessageHandler() {
                requireNonNull(processorSingletons);
                return new ActivationCommandPollerHandler(plugin, stateFactory.baseState(),
                        processorSingletons.get().commandHandler());
            }

            @Override
            public AgentStep commandPollerStep() {
                return singletons.get().commandPollerStep();
            }
        };
    }

    private static boolean hasCommandStore(final AppConfig appConfig) {
        return appConfig.appType().hasCommandStore(appConfig);
    }
}
