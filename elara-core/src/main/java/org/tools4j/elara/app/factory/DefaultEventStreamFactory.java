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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.EventStreamConfig;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.state.EventProcessingState.MutableEventProcessingState;
import org.tools4j.elara.app.state.TransientEventState;
import org.tools4j.elara.event.CompositeEventProcessor;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.EventProcessorHandler;
import org.tools4j.elara.output.OutputEventProcessor;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.EventReceiverStep;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultEventStreamFactory implements EventStreamFactory {
    private final AppConfig appConfig;
    private final EventStreamConfig eventStreamConfig;
    private final MutableEventProcessingState eventProcessingState;
    private final Installer[] plugins;
    private final Supplier<? extends EventStreamFactory> eventStreamSingletons;
    private final Supplier<? extends CommandStreamFactory> commandStreamSingletons;
    private final Supplier<? extends OutputFactory> outputSingletons;

    public DefaultEventStreamFactory(final AppConfig appConfig,
                                     final EventStreamConfig eventStreamConfig,
                                     final MutableEventProcessingState eventProcessingState,
                                     final Installer[] plugins,
                                     final Supplier<? extends CommandStreamFactory> commandStreamSingletons,
                                     final Supplier<? extends OutputFactory> outputSingletons,
                                     final Supplier<? extends EventStreamFactory> eventStreamSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.eventStreamConfig = requireNonNull(eventStreamConfig);
        this.eventProcessingState = requireNonNull(eventProcessingState);
        this.plugins = requireNonNull(plugins);
        this.commandStreamSingletons = requireNonNull(commandStreamSingletons);
        this.outputSingletons = requireNonNull(outputSingletons);
        this.eventStreamSingletons = requireNonNull(eventStreamSingletons);
    }

    @Override
    public TransientEventState transientEventState() {
        throw new RuntimeException("not implemented");
        //return new DefaultTransientEventState();
    }

    @Override
    public EventProcessor eventProcessor() {
        final EventProcessor eventProcessor = eventStreamConfig.eventProcessor();
        final EventProcessor outputProcessor = OutputEventProcessor.create(
                outputSingletons.get().output(), appConfig.exceptionHandler()
        );
        if (plugins.length == 0 && (eventProcessor == EventProcessor.NOOP || outputProcessor == EventProcessor.NOOP)) {
            return eventProcessor == EventProcessor.NOOP ? outputProcessor : eventProcessor;
        }
        final EventProcessor[] processors = new EventProcessor[plugins.length + 2];
        int count = 0;
        for (final Installer plugin : plugins) {
            processors[count] = plugin.eventProcessor(eventProcessingState);
            if (processors[count] != EventProcessor.NOOP) {
                count++;
            }
        }
        //application processor second last
        if (eventProcessor != EventProcessor.NOOP) {
            processors[count++] = eventProcessor;
        }
        //output processor last
        if (outputProcessor != EventProcessor.NOOP) {
            processors[count++] = outputProcessor;
        }
        if (count == 0) {
            return EventProcessor.NOOP;
        }
        return new CompositeEventProcessor(
                count == processors.length ? processors : Arrays.copyOf(processors, count)
        );
    }

    @Override
    public EventHandler eventHandler() {
        return new EventProcessorHandler(
                eventProcessingState,
                eventStreamSingletons.get().eventProcessor(),
                commandStreamSingletons.get().sourceContextProvider().sourceContext(eventStreamConfig.sourceId()),
                appConfig.exceptionHandler(),
                appConfig.duplicateHandler()
        );
    }

    @Override
    public AgentStep eventPollerStep() {
        return new EventReceiverStep(
                eventStreamConfig.eventReceiver(),
                eventStreamSingletons.get().eventHandler()
        );
    }
}
