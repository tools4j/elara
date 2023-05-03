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

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.send.SenderSupplier.SentListener;
import org.tools4j.elara.sequence.SequenceGenerator;

import static java.util.Objects.requireNonNull;

public class DefaultSourceContext implements SourceContext {

    private final int sourceId;
    private final SenderSupplier senderSupplier;
    private final CommandTracker commandTracker;
    private final SequenceGenerator sequenceGenerator;
    private final SentListener sentListener;

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
        this.senderSupplier = requireNonNull(senderSupplier);
        this.commandTracker = new DefaultCommandTracker(this, sourceSequenceGenerator, baseState);
        this.sequenceGenerator = commandTracker.transientCommandState().sourceSequenceGenerator();
        this.sentListener = ((DefaultCommandTracker)commandTracker)::notifyCommandSent;
    }

    @Override
    public int sourceId() {
        return sourceId;
    }

    @Override
    public CommandTracker commandTracker() {
        return commandTracker;
    }

    @Override
    public CommandSender commandSender() {
        return senderSupplier.senderFor(sourceId, sequenceGenerator, sentListener);
    }

    @Override
    public String toString() {
        return "DefaultSourceContext{" +
                "sourceId=" + sourceId +
                "|commandTracker=" + commandTracker +
                '}';
    }
}
