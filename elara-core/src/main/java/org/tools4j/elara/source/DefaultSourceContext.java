/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.source;

import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.InFlightState;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.sequence.SequenceGenerator;

import static java.util.Objects.requireNonNull;

public class DefaultSourceContext implements SourceContext {

    private final int sourceId;
    private final BaseState baseState;
    private final CommandSender commandSender;
    private final InFlightState inFlightState = new InFlightStateImpl();

    private final SequenceGenerator sourceSequenceGenerator;

    public DefaultSourceContext(final int sourceId,
                                final BaseState baseState,
                                final SenderSupplier senderSupplier) {
        this(sourceId, baseState, senderSupplier, SequenceGenerator.create());
    }

    public DefaultSourceContext(final int sourceId,
                                final BaseState baseState,
                                final SenderSupplier senderSupplier,
                                final SequenceGenerator sourceSequenceGenerator) {
        this.sourceId = sourceId;
        this.baseState = requireNonNull(baseState);
        this.commandSender = senderSupplier.senderFor(sourceId, sourceSequenceGenerator);
        this.sourceSequenceGenerator = requireNonNull(sourceSequenceGenerator);
    }

    @Override
    public int sourceId() {
        return sourceId;
    }

    SequenceGenerator sourceSequenceGenerator() {
        return sourceSequenceGenerator;
    }

    @Override
    public CommandSender commandSender() {
        return commandSender;
    }

    @Override
    public InFlightState inFlightState() {
        return inFlightState;
    }

    @Override
    public String toString() {
        return "DefaultSourceContext{" +
                "sourceId=" + sourceId +
                "|sourceSequence=" + sourceSequenceGenerator.sequence() +
                '}';
    }

    private class InFlightStateImpl implements InFlightState {

        @Override
        public CommandSendingState commandLastSent() {
            return null;//FIXME
        }

        @Override
        public EventProcessingState eventLastProcessed() {
            return null;//FIXME
        }

        @Override
        public boolean hasInFlightCommand() {
            return baseState.lastAppliedCommandSequence(sourceId) < sourceSequenceGenerator.sequence() - 1;
        }
    }
}
