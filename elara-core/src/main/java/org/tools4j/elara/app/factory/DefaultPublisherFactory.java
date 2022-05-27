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
import org.tools4j.elara.handler.DefaultOutputHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.PublisherStep;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.send.SenderSupplier.LOOPBACK_SOURCE;
import static org.tools4j.elara.step.PublisherStep.DEFAULT_POLLER_ID;

public class DefaultPublisherFactory implements PublisherFactory {
    private final AppConfig appConfig;
    private final ProcessorConfig processorConfig;
    private final Supplier<? extends PublisherFactory> publisherSingletons;
    private final Supplier<? extends SequencerFactory> sequencerSingletons;
    private final Supplier<? extends InOutFactory> inOutSingletons;

    public DefaultPublisherFactory(final AppConfig appConfig,
                                   final ProcessorConfig processorConfig,
                                   final Supplier<? extends PublisherFactory> publisherSingletons,
                                   final Supplier<? extends SequencerFactory> sequencerSingletons,
                                   final Supplier<? extends InOutFactory> inOutSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.processorConfig = requireNonNull(processorConfig);
        this.publisherSingletons = requireNonNull(publisherSingletons);
        this.sequencerSingletons = requireNonNull(sequencerSingletons);
        this.inOutSingletons = requireNonNull(inOutSingletons);
    }

    @Override
    public CommandSender loopbackCommandSender() {
        return sequencerSingletons.get().senderSupplier().senderFor(
                LOOPBACK_SOURCE, appConfig.timeSource().currentTime()
        );
    }

    @Override
    public OutputHandler outputHandler() {
        return new DefaultOutputHandler(
                inOutSingletons.get().output(), publisherSingletons.get().loopbackCommandSender(), appConfig.exceptionHandler()
        );
    }

    @Override
    public AgentStep publisherStep() {
        final OutputHandler outputHandler = publisherSingletons.get().outputHandler();
        try {
            return new PublisherStep(outputHandler, processorConfig.eventStore(), DEFAULT_POLLER_ID);
        } catch (final UnsupportedOperationException e) {
            //ignore, use non-tracking below
        }
        return new PublisherStep(outputHandler, processorConfig.eventStore());
    }
}
