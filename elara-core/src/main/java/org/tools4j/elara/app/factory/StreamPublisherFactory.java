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
import org.tools4j.elara.app.config.EventStreamConfig;
import org.tools4j.elara.handler.DefaultOutputHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.send.CommandHandlingSender;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.StreamPublisherStep;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.stream.MessageStream;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.step.AgentStep.NOOP;

public class StreamPublisherFactory implements PublisherFactory {
    private final AppConfig appConfig;
    private final EventStreamConfig eventStreamConfig;
    private final Supplier<? extends PublisherFactory> publisherSingletons;
    private final Supplier<? extends InOutFactory> inOutSingletons;
    private final Supplier<? extends PluginFactory> pluginSingletons;

    public StreamPublisherFactory(final AppConfig appConfig,
                                  final EventStreamConfig eventStreamConfig,
                                  final Supplier<? extends PublisherFactory> publisherSingletons,
                                  final Supplier<? extends InOutFactory> inOutSingletons,
                                  final Supplier<? extends PluginFactory> pluginSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.eventStreamConfig = requireNonNull(eventStreamConfig);
        this.publisherSingletons = requireNonNull(publisherSingletons);
        this.inOutSingletons = requireNonNull(inOutSingletons);
        this.pluginSingletons = requireNonNull(pluginSingletons);
    }

    @Override
    public CommandSender loopbackCommandSender() {
        //FIXME this is NOOP loopback sender
        return new CommandHandlingSender(64, appConfig.timeSource(), command -> Result.POLL);
    }

    @Override
    public OutputHandler outputHandler() {
        return new DefaultOutputHandler(
                inOutSingletons.get().output(), publisherSingletons.get().loopbackCommandSender(), appConfig.exceptionHandler()
        );
    }

    @Override
    public AgentStep publisherStep() {
        if (inOutSingletons.get().output() == Output.NOOP) {
            return NOOP;
        }
        final MessageStream eventStream = eventStreamConfig.eventStream();
        final OutputHandler outputHandler = publisherSingletons.get().outputHandler();
        return new StreamPublisherStep(outputHandler, eventStream, appConfig.exceptionHandler());
    }
}