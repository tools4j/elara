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
import org.tools4j.elara.init.Context;
import org.tools4j.elara.loop.CommandPollerStep;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.nobark.loop.Step;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class DefaultProcessorFactory implements ProcessorFactory {

    private final ElaraFactory elaraFactory;

    public DefaultProcessorFactory(final ElaraFactory elaraFactory) {
        this.elaraFactory = requireNonNull(elaraFactory);
    }

    protected ElaraFactory elaraFactory() {
        return elaraFactory;
    }

    protected Context context() {
        return elaraFactory.context();
    }

    @Override
    public CommandProcessor commandProcessor() {
        final CommandProcessor commandProcessor = context().commandProcessor();
        final Plugin.Context[] plugins = elaraFactory().pluginFactory().plugins();
        if (plugins.length == 0) {
            return commandProcessor;
        }
        final BaseState baseState = elaraFactory().pluginFactory().baseState();
        final CommandProcessor[] processors = new CommandProcessor[plugins.length + 1];
        int count = 1;
        for (final Plugin.Context plugin : plugins) {
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
                elaraFactory().pluginFactory().baseState(),
                new DefaultEventRouter(context().eventLog().appender(), elaraFactory().applierFactory().eventHandler()),
                commandProcessor(),
                context().exceptionHandler(),
                context().duplicateHandler()
        );
    }

    @Override
    public Step commandPollerStep() {
        return new CommandPollerStep(context().commandLog().poller(), new PollerCommandHandler(commandHandler()));
    }


}
