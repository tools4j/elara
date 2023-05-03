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
package org.tools4j.elara.send;

import org.tools4j.elara.sequence.SequenceGenerator;

import static java.util.Objects.requireNonNull;

abstract class FlyweightCommandSender implements CommandSender.Default, SenderSupplier {

    private int sourceId;
    private SequenceGenerator sourceSequenceGenerator;
    private SentListener sentListener;

    @Override
    public CommandSender senderFor(final int sourceId,
                                   final SequenceGenerator sourceSequenceGenerator,
                                   final SentListener sentListener) {
        this.sourceId = sourceId;
        this.sourceSequenceGenerator = requireNonNull(sourceSequenceGenerator);
        this.sentListener = requireNonNull(sentListener);
        return this;
    }

    void notifySent(final long commandTime) {
        sentListener.onSent(sourceSequenceGenerator.sequence(), commandTime);
        sourceSequenceGenerator.nextSequence();
    }

    @Override
    public int sourceId() {
        return sourceId;
    }

    @Override
    public long nextCommandSequence() {
        return sourceSequenceGenerator.sequence();
    }
}
