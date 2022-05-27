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
import org.tools4j.elara.app.config.CommandStoreConfig;
import org.tools4j.elara.send.CommandAppendingSender;
import org.tools4j.elara.send.DefaultSenderSupplier;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.SequencerStep;
import org.tools4j.elara.store.MessageStore;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class AppendingSequencerFactory implements SequencerFactory {

    private final AppConfig appConfig;
    private final CommandStoreConfig commandStoreConfig;
    private final Supplier<? extends SequencerFactory> sequencerSingletons;
    private final Supplier<? extends InOutFactory> inOutSingletons;

    public AppendingSequencerFactory(final AppConfig appConfig,
                                     final CommandStoreConfig commandStoreConfig,
                                     final Supplier<? extends SequencerFactory> sequencerSingletons,
                                     final Supplier<? extends InOutFactory> inOutSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.commandStoreConfig = requireNonNull(commandStoreConfig);
        this.sequencerSingletons = requireNonNull(sequencerSingletons);
        this.inOutSingletons = requireNonNull(inOutSingletons);
    }

    @Override
    public SenderSupplier senderSupplier() {
        final MessageStore.Appender commandAppender = commandStoreConfig.commandStore().appender();
        return new DefaultSenderSupplier(new CommandAppendingSender(appConfig.timeSource(), commandAppender));
    }

    @Override
    public AgentStep sequencerStep() {
        return new SequencerStep(sequencerSingletons.get().senderSupplier(), inOutSingletons.get().inputs());
    }

}
