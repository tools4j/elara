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

import org.agrona.collections.Long2LongCounterMap;
import org.tools4j.elara.command.SourceIds;

import static java.util.Objects.requireNonNull;

public class DefaultSenderSupplier implements SenderSupplier {

    public static final long INITIAL_SEQUENCE = 0;

    private final FlyweightCommandSender commandSender;
    private final Long2LongCounterMap sequenceBySource;

    public DefaultSenderSupplier(final FlyweightCommandSender commandSender) {
        this.commandSender = requireNonNull(commandSender);
        this.sequenceBySource = new Long2LongCounterMap(INITIAL_SEQUENCE);
    }

    public DefaultSenderSupplier(final FlyweightCommandSender commandSender,
                                 final int initialCapacity,
                                 final float loadFactor ) {
        this.commandSender = requireNonNull(commandSender);
        this.sequenceBySource = new Long2LongCounterMap(initialCapacity, loadFactor, INITIAL_SEQUENCE);
    }

    @Override
    public CommandSender senderFor(final CharSequence sourceId) {
        return senderFor(SourceIds.toInt(sourceId));
    }

    @Override
    public CommandSender senderFor(final int sourceId) {
        return senderFor(sourceId, sequenceBySource.getAndIncrement(sourceId));
    }

    @Override
    public CommandSender senderFor(final CharSequence sourceId, final long sourceSeq) {
        return senderFor(SourceIds.toInt(sourceId), sourceSeq);
    }

    @Override
    public CommandSender senderFor(final int sourceId, final long sourceSeq) {
        return commandSender.init(sourceId, sourceSeq);
    }
}
