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
import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.app.config.ProcessorConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.command.CompositeCommandProcessor;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.DefaultCommandHandler;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultProcessorFactory implements ProcessorFactory {

    private final AppConfig appConfig;
    private final ProcessorConfig processorConfig;
    private final EventStoreConfig eventStoreConfig;
    private final Supplier<? extends ProcessorFactory> processorSingletons;
    private final Supplier<? extends ApplierFactory> applierSingletons;
    private final Supplier<? extends PluginFactory> pluginSingletons;

    public DefaultProcessorFactory(final AppConfig appConfig,
                                   final ProcessorConfig processorConfig,
                                   final EventStoreConfig eventStoreConfig,
                                   final Supplier<? extends ProcessorFactory> processorSingletons,
                                   final Supplier<? extends ApplierFactory> applierSingletons,
                                   final Supplier<? extends PluginFactory> pluginSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.processorConfig = requireNonNull(processorConfig);
        this.eventStoreConfig = requireNonNull(eventStoreConfig);
        this.processorSingletons = requireNonNull(processorSingletons);
        this.applierSingletons = requireNonNull(applierSingletons);
        this.pluginSingletons = requireNonNull(pluginSingletons);
    }

    @Override
    public CommandProcessor commandProcessor() {
        final CommandProcessor commandProcessor = processorConfig.commandProcessor();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = pluginSingletons.get().plugins();
        if (plugins.length == 0) {
            return commandProcessor;
        }
        final BaseState baseState = pluginSingletons.get().baseState();
        final CommandProcessor[] processors = new CommandProcessor[plugins.length + 1];
        int count = 1;
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            processors[count] = plugin.commandProcessor(baseState);
            if (processors[count] != CommandProcessor.NOOP) {
                count++;
            }
        }
        if (count == 1) {
            return commandProcessor;
        }
        processors[0] = commandProcessor;//application processor first
        return new CompositeCommandProcessor(
                count == processors.length ? processors : Arrays.copyOf(processors, count)
        );
    }

    @Override
    public CommandHandler commandHandler() {
        return new DefaultCommandHandler(
                pluginSingletons.get().baseState(),
                new DefaultEventRouter(
                        appConfig.timeSource(),
                        pluginSingletons.get().baseState(),
                        eventStoreConfig.eventStore().appender(),
                        applierSingletons.get().eventHandler()
                ),
                processorSingletons.get().commandProcessor(),
                appConfig.exceptionHandler(),
                eventStoreConfig.duplicateHandler()
        );
    }
}
