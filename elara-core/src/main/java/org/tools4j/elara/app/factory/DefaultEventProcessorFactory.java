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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.EventProcessorConfig;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.state.MutableEventProcessingState;
import org.tools4j.elara.composite.CompositeEventProcessor;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.EventProcessorHandler;
import org.tools4j.elara.handler.Handlers;
import org.tools4j.elara.handler.RetryOutputHandler;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;
import org.tools4j.elara.source.CommandSource;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultEventProcessorFactory implements EventProcessorFactory {
    private static final int MAX_OUTPUT_RETRIES = 3;
    private final AppConfig appConfig;
    private final EventProcessorConfig eventProcessorConfig;
    private final MutableEventProcessingState eventProcessingState;
    private final Installer[] plugins;
    private final Supplier<? extends EventProcessorFactory> eventProcessorSingletons;
    private final Supplier<? extends CommandSenderFactory> commandStreamSingletons;
    private final Supplier<? extends OutputFactory> outputSingletons;

    public DefaultEventProcessorFactory(final AppConfig appConfig,
                                        final EventProcessorConfig eventProcessorConfig,
                                        final MutableEventProcessingState eventProcessingState,
                                        final Installer[] plugins,
                                        final Supplier<? extends EventProcessorFactory> eventProcessorSingletons,
                                        final Supplier<? extends CommandSenderFactory> commandStreamSingletons,
                                        final Supplier<? extends OutputFactory> outputSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.eventProcessorConfig = requireNonNull(eventProcessorConfig);
        this.eventProcessingState = requireNonNull(eventProcessingState);
        this.plugins = requireNonNull(plugins);
        this.eventProcessorSingletons = requireNonNull(eventProcessorSingletons);
        this.commandStreamSingletons = requireNonNull(commandStreamSingletons);
        this.outputSingletons = requireNonNull(outputSingletons);
    }

    @Override
    public CommandSource processorSource() {
        final int processorSourceId = eventProcessorConfig.processorSourceId();
        return commandStreamSingletons.get().commandSourceProvider().sourceById(processorSourceId);
    }

    private EventProcessor outputEventProcessor(final Output output) {
        return Handlers.asEventProcessor(
                RetryOutputHandler.create(MAX_OUTPUT_RETRIES, output, appConfig.exceptionHandler()),
                eventProcessingState
        );
    }

    @Override
    public EventProcessor eventProcessor() {
        final EventProcessor eventProcessor = eventProcessorConfig.eventProcessor();
        final Output output = outputSingletons.get().output();
        if (plugins.length == 0 && output == Output.NOOP) {
            return eventProcessor;
        }
        final EventProcessor[] processors = new EventProcessor[plugins.length + 2];
        for (int i = 0; i < plugins.length; i++) {
            processors[i] = plugins[i].eventProcessor(eventProcessingState);
        }
        processors[plugins.length] = eventProcessor;//application processor last
        processors[plugins.length + 1] = outputEventProcessor(output);//output very last
        return CompositeEventProcessor.create(processors);
    }

    @Override
    public EventHandler eventHandler() {
        return new EventProcessorHandler(
                eventProcessingState,
                commandStreamSingletons.get().commandContext(),
                eventProcessorSingletons.get().processorSource(),
                eventProcessorSingletons.get().eventProcessor(),
                appConfig.exceptionHandler(),
                eventProcessorConfig.duplicateHandler()
        );
    }
}
