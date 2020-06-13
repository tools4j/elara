/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.command.CompositeCommandProcessor;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.DefaultCommandHandler;
import org.tools4j.elara.handler.PollerCommandHandler;
import org.tools4j.elara.init.CommandLogMode;
import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.log.MessageLog.Poller;
import org.tools4j.elara.loop.CommandPollerStep;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.nobark.loop.Step;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class DefaultProcessorFactory implements ProcessorFactory {

    private final Configuration configuration;
    private final Singletons singletons;

    public DefaultProcessorFactory(final Configuration configuration, final Singletons singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public CommandProcessor commandProcessor() {
        final CommandProcessor commandProcessor = configuration.commandProcessor();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = singletons.plugins();
        if (plugins.length == 0) {
            return commandProcessor;
        }
        final BaseState baseState = singletons.baseState();
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
                singletons.baseState(),
                new DefaultEventRouter(configuration.eventLog().appender(), singletons.eventHandler()),
                singletons.commandProcessor(),
                configuration.exceptionHandler(),
                configuration.duplicateHandler()
        );
    }

    @Override
    public Step commandPollerStep() {
        final Poller commandLogPoller;
        switch (configuration.commandLogMode()) {
            case REPLAY_ALL:
                commandLogPoller = configuration.commandLog().poller();
                break;
            case FROM_LAST:
                commandLogPoller = configuration.commandLog().poller(CommandLogMode.DEFAULT_POLLER_ID);
                break;
            case FROM_END:
                commandLogPoller = configuration.commandLog().poller();
                commandLogPoller.moveToEnd();
                break;
            default:
                throw new IllegalArgumentException("Unsupported command log mode: " + configuration.commandLogMode());
        }
        return new CommandPollerStep(commandLogPoller, new PollerCommandHandler(singletons.commandHandler()));
    }


}
