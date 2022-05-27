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
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.send.CommandHandlingSender;
import org.tools4j.elara.send.DefaultSenderSupplier;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.SequencerStep;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ProcessingSequencerFactory implements SequencerFactory {

    private final AppConfig appConfig;
    private final Supplier<? extends SequencerFactory> sequencerSingletons;
    private final Supplier<? extends ProcessorFactory> processorSingletons;
    private final Supplier<? extends InOutFactory> inOutSingletons;

    public ProcessingSequencerFactory(final AppConfig appConfig,
                                      final Supplier<? extends SequencerFactory> sequencerSingletons,
                                      final Supplier<? extends ProcessorFactory> processorSingletons,
                                      final Supplier<? extends InOutFactory> inOutSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.sequencerSingletons = requireNonNull(sequencerSingletons);
        this.processorSingletons = requireNonNull(processorSingletons);
        this.inOutSingletons = requireNonNull(inOutSingletons);
    }

    @Override
    public SenderSupplier senderSupplier() {
        final CommandHandler commandHandler = processorSingletons.get().commandHandler();
        return new DefaultSenderSupplier(new CommandHandlingSender(4096, appConfig.timeSource(), commandHandler));
    }

    @Override
    public AgentStep sequencerStep() {
        return new SequencerStep(sequencerSingletons.get().senderSupplier(), inOutSingletons.get().inputs());
    }
}
