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
package org.tools4j.elara.plugin.timer;

import org.tools4j.elara.app.factory.EventStreamFactory;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.ProcessorFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.plugin.timer.TimerController.ControlContext;
import org.tools4j.elara.route.CommandTransaction;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.stream.MessageStream;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class TimerPluginInterceptor implements Interceptor {

    private final TimerPlugin plugin;

    TimerPluginInterceptor(final TimerPlugin plugin) {
        this.plugin = requireNonNull(plugin);
    }

    @Override
    public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
        requireNonNull(singletons);
        return new ProcessorFactory() {
            @Override
            public CommandProcessor commandProcessor() {
                final CommandProcessor commandProcessor = singletons.get().commandProcessor();
                return (command, router) -> {
                    try (final ControlContext ignored = plugin.controller(router)) {
                        commandProcessor.onCommand(command, router);
                    }
                };
            }

            @Override
            public CommandTransaction commandTransaction() {
                return singletons.get().commandTransaction();
            }

            @Override
            public CommandHandler commandHandler() {
                return singletons.get().commandHandler();
            }
        };
    }

    @Override
    public EventStreamFactory eventStreamFactory(final Supplier<? extends EventStreamFactory> singletons) {
        requireNonNull(singletons);
        return new EventStreamFactory() {
            @Override
            public MessageStream eventStream() {
                return singletons.get().eventStream();
            }

            @Override
            public EventProcessor eventProcessor() {
                final EventProcessor eventProcessor = singletons.get().eventProcessor();
                return (event, inFlightState, sender) -> {
                    try (final ControlContext ignored = plugin.controller(event, sender)) {
                        eventProcessor.onEvent(event, inFlightState, sender);
                    }
                };
            }

            @Override
            public AgentStep eventStep() {
                return singletons.get().eventStep();
            }
        };
    }
}
