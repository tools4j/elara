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
import org.tools4j.elara.app.config.ApplierConfig;
import org.tools4j.elara.app.config.CommandProcessorConfig;
import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.composite.CompositeCommandProcessor;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.DeduplicatingCommandHandler;
import org.tools4j.elara.handler.ProcessingCommandHandler;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;
import org.tools4j.elara.route.CommandTransaction;
import org.tools4j.elara.route.DefaultEventRouter;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultCommandProcessorFactory implements CommandProcessorFactory {

    private final AppConfig appConfig;
    private final CommandProcessorConfig commandProcessorConfig;
    private final EventStoreConfig eventStoreConfig;
    private final ApplierConfig applierConfig;
    private final BaseState baseState;
    private final Installer[] plugins;
    private final Supplier<? extends CommandProcessorFactory> commandProcessorSingletons;
    private final Supplier<? extends ApplierFactory> applierSingletons;

    public DefaultCommandProcessorFactory(final AppConfig appConfig,
                                          final CommandProcessorConfig commandProcessorConfig,
                                          final EventStoreConfig eventStoreConfig,
                                          final ApplierConfig applierConfig,
                                          final BaseState baseState,
                                          final Installer[] plugins,
                                          final Supplier<? extends CommandProcessorFactory> commandProcessorSingletons,
                                          final Supplier<? extends ApplierFactory> applierSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.applierConfig = requireNonNull(applierConfig);
        this.commandProcessorConfig = requireNonNull(commandProcessorConfig);
        this.eventStoreConfig = requireNonNull(eventStoreConfig);
        this.baseState = requireNonNull(baseState);
        this.plugins = requireNonNull(plugins);
        this.commandProcessorSingletons = requireNonNull(commandProcessorSingletons);
        this.applierSingletons = requireNonNull(applierSingletons);
    }

    @Override
    public CommandProcessor commandProcessor() {
        final CommandProcessor commandProcessor = commandProcessorConfig.commandProcessor();
        if (plugins.length == 0) {
            return commandProcessor;
        }
        final CommandProcessor[] processors = new CommandProcessor[plugins.length + 1];
        for (int i = 0; i < plugins.length; i++) {
            processors[i + 1] = plugins[i].commandProcessor(baseState);
        }
        processors[0] = commandProcessor;//application processor first
        return CompositeCommandProcessor.create(processors);
    }

    @Override
    public CommandTransaction commandTransaction() {
        return new DefaultEventRouter(
                appConfig.timeSource(),
                baseState,
                eventStoreConfig.eventStore().appender(),
                applierSingletons.get().eventHandler()
        );
    }

    @Override
    public CommandHandler commandHandler() {
        return new DeduplicatingCommandHandler(
                baseState,
                new ProcessingCommandHandler(
                        commandProcessorSingletons.get().commandTransaction(),
                        commandProcessorSingletons.get().commandProcessor()),
                appConfig.exceptionHandler(),
                applierConfig.duplicateHandler());
    }
}
