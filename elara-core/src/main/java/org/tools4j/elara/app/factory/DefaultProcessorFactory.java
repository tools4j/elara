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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ProcessorConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.command.CompositeCommandProcessor;
import org.tools4j.elara.event.CompositeEventApplier;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.DefaultCommandHandler;
import org.tools4j.elara.handler.DefaultEventHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.EventReplayStep;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultProcessorFactory implements ProcessorFactory {

    private final AppConfig appConfig;
    private final ProcessorConfig processorConfig;
    private final Supplier<? extends ProcessorFactory> processorSingletons;
    private final Supplier<? extends PluginFactory> pluginSingletons;

    public DefaultProcessorFactory(final AppConfig appConfig,
                                   final ProcessorConfig processorConfig,
                                   final Supplier<? extends ProcessorFactory> processorSingletons,
                                   final Supplier<? extends PluginFactory> pluginSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.processorConfig = requireNonNull(processorConfig);
        this.processorSingletons = requireNonNull(processorSingletons);
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
                        processorConfig.eventStore().appender(),
                        processorSingletons.get().eventHandler()
                ),
                processorSingletons.get().commandProcessor(),
                appConfig.exceptionHandler(),
                appConfig.duplicateHandler()
        );
    }

    @Override
    public EventApplier eventApplier() {
        final EventApplier eventApplier = processorConfig.eventApplier();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = pluginSingletons.get().plugins();
        if (plugins.length == 0) {
            return eventApplier;
        }
        final BaseState.Mutable baseState = pluginSingletons.get().baseState();
        final EventApplier[] appliers = new EventApplier[plugins.length + 1];
        int count = 0;
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            appliers[count] = plugin.eventApplier(baseState);
            if (appliers[count] != EventApplier.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return eventApplier;
        }
        appliers[count++] = eventApplier;//application applier last
        return new CompositeEventApplier(
                count == appliers.length ? appliers : Arrays.copyOf(appliers, count)
        );
    }

    @Override
    public EventHandler eventHandler() {
        return new DefaultEventHandler(
                pluginSingletons.get().baseState(),
                processorSingletons.get().eventApplier(),
                appConfig.exceptionHandler(),
                appConfig.duplicateHandler()
        );
    }

    @Override
    public AgentStep eventPollerStep() {
        return new EventReplayStep(
                processorConfig.eventStore().poller(), processorSingletons.get().eventHandler()
        );
    }
}
