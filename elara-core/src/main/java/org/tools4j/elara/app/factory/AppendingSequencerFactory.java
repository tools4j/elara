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
import org.tools4j.elara.app.config.CommandStoreConfig;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.send.CommandAppendingSender;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.DefaultCommandContext;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.source.CommandSourceProvider;
import org.tools4j.elara.source.DefaultCommandSourceProvider;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.app.state.MutableInFlightState.NO_IN_FLIGHT_STATE;

public class AppendingSequencerFactory implements SequencerFactory {

    private final AppConfig appConfig;
    private final CommandStoreConfig commandStoreConfig;
    private final BaseState baseState;
    private final Supplier<? extends SequencerFactory> sequencerSingletons;
    private final Supplier<? extends InputFactory> inputSingletons;

    public AppendingSequencerFactory(final AppConfig appConfig,
                                     final CommandStoreConfig commandStoreConfig,
                                     final BaseState baseState,
                                     final Supplier<? extends SequencerFactory> sequencerSingletons,
                                     final Supplier<? extends InputFactory> inputSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.commandStoreConfig = requireNonNull(commandStoreConfig);
        this.baseState = requireNonNull(baseState);
        this.sequencerSingletons = requireNonNull(sequencerSingletons);
        this.inputSingletons = requireNonNull(inputSingletons);
    }

    @Override
    public CommandContext commandContext() {
        return new DefaultCommandContext(NO_IN_FLIGHT_STATE, sequencerSingletons.get().commandSourceProvider());
    }

    @Override
    public CommandSourceProvider commandSourceProvider() {
        return new DefaultCommandSourceProvider(baseState, NO_IN_FLIGHT_STATE, sequencerSingletons.get().senderSupplier());
    }

    @Override
    public SenderSupplier senderSupplier() {
        final MessageStore.Appender commandAppender = commandStoreConfig.commandStore().appender();
        return new CommandAppendingSender(appConfig.timeSource(), commandAppender);
    }

    @Override
    public AgentStep sequencerStep() {
        return inputSingletons.get().input().inputPollerStep(sequencerSingletons.get().commandContext());
    }

}
