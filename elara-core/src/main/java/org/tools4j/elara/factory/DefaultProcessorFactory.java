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
package org.tools4j.elara.factory;

import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.config.Configuration;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.command.CompositeCommandProcessor;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.CommandPollerHandler;
import org.tools4j.elara.handler.DefaultCommandHandler;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.CommandPollerStep;
import org.tools4j.elara.store.MessageStore.Poller;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultProcessorFactory implements ProcessorFactory {

    private final Configuration configuration;
    private final Supplier<? extends Singletons> singletons;

    public DefaultProcessorFactory(final Configuration configuration, final Supplier<? extends Singletons> singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public CommandProcessor commandProcessor() {
        final CommandProcessor commandProcessor = configuration.commandProcessor();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = singletons.get().plugins();
        if (plugins.length == 0) {
            return commandProcessor;
        }
        final BaseState baseState = singletons.get().baseState();
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
        final Singletons singletons = this.singletons.get();
        return new DefaultCommandHandler(
                singletons.baseState(),
                new DefaultEventRouter(
                        configuration.timeSource(), configuration.eventStore().appender(), singletons.eventHandler()
                ),
                singletons.commandProcessor(),
                configuration.exceptionHandler(),
                configuration.duplicateHandler()
        );
    }

    @Override
    public AgentStep commandPollerStep() {
        final Poller commandStorePoller;
        switch (configuration.commandPollingMode()) {
            case REPLAY_ALL:
                commandStorePoller = configuration.commandStore().poller();
                break;
            case FROM_LAST:
                commandStorePoller = configuration.commandStore().poller(CommandPollingMode.DEFAULT_POLLER_ID);
                break;
            case FROM_END:
                commandStorePoller = configuration.commandStore().poller();
                commandStorePoller.moveToEnd();
                break;
            case NO_STORE:
                return AgentStep.NO_OP;
            default:
                throw new IllegalArgumentException("Unsupported command polling mode: " + configuration.commandPollingMode());
        }
        return new CommandPollerStep(commandStorePoller, new CommandPollerHandler(singletons.get().commandHandler()));
    }


}
