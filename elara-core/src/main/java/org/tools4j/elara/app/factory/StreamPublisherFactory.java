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
import org.tools4j.elara.app.config.EventReceiverConfig;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.handler.RetryOutputHandler;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.ReceiverPublisherStep;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.step.AgentStep.NOOP;

public class StreamPublisherFactory implements PublisherFactory {
    private static final int MAX_OUTPUT_RETRIES = 3;
    private final AppConfig appConfig;
    private final EventReceiverConfig eventReceiverConfig;
    private final Supplier<? extends PublisherFactory> publisherSingletons;
    private final Supplier<? extends OutputFactory> outputSingletons;

    public StreamPublisherFactory(final AppConfig appConfig,
                                  final EventReceiverConfig eventReceiverConfig,
                                  final Supplier<? extends PublisherFactory> publisherSingletons,
                                  final Supplier<? extends OutputFactory> outputSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.eventReceiverConfig = requireNonNull(eventReceiverConfig);
        this.publisherSingletons = requireNonNull(publisherSingletons);
        this.outputSingletons = requireNonNull(outputSingletons);
    }

    @Override
    public OutputHandler outputHandler() {
        return RetryOutputHandler.create(
                MAX_OUTPUT_RETRIES, outputSingletons.get().output(), appConfig.exceptionHandler()
        );
    }

    @Override
    public AgentStep publisherStep() {
        final OutputHandler outputHandler = publisherSingletons.get().outputHandler();
        if (outputHandler == OutputHandler.NOOP) {
            return NOOP;
        }
        return new ReceiverPublisherStep(outputHandler, eventReceiverConfig.eventReceiver());
    }
}
