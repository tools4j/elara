/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.wire.DocumentContext;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.log.Flyweight;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.PeekableMessageLog;
import org.tools4j.elara.log.PeekableMessageLog.PeekPollHandler.Result;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.log.PeekableMessageLog.PeekPollHandler.Result.POLL;

public class ChronicleLogPoller<M> implements PeekableMessageLog.PeekablePoller<M> {

    private final ExcerptTailer tailer;
    private final Flyweight<? extends M> flyweight;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    public ChronicleLogPoller(final ChronicleQueue queue, final Flyweight<? extends M> flyweight) {
        this(queue.createTailer(), flyweight);
    }

    public ChronicleLogPoller(final String id,
                              final ChronicleQueue queue,
                              final Flyweight<? extends M> flyweight) {
        this(queue.createTailer(id), flyweight);
    }

    public ChronicleLogPoller(final ExcerptTailer tailer, final Flyweight<? extends M> flyweight) {
        this.tailer = requireNonNull(tailer);
        this.flyweight = requireNonNull(flyweight);
    }

    @Override
    public long entryId() {
        return tailer.index();
    }

    @Override
    public boolean moveTo(final long entryId) {
        return tailer.moveToIndex(entryId);
    }

    @Override
    public ChronicleLogPoller<M> moveToStart() {
        tailer.toStart();
        return this;
    }

    @Override
    public PeekableMessageLog.PeekablePoller<M> moveToEnd() {
        tailer.toEnd();
        return this;
    }

    @Override
    public boolean moveToNext() {
        boolean present;
        do {
            try (final DocumentContext context = tailer.readingDocument()) {
                if (context.isData()) {
                    return true;
                }
                present = context.isPresent();
            }
        } while (present);
        return false;
    }

    @Override
    public int poll(final MessageLog.Handler<? super M> handler) {
        try (final DocumentContext context = tailer.readingDocument()) {
            if (context.isData()) {
                final Bytes<?> bytes = context.wire().bytes();
                final int size = bytes.readInt();
                final int offset = (int)bytes.readPosition();
                final long addr = bytes.addressForRead(offset);
                buffer.wrap(addr, size);
                final M flyMessage = flyweight.init(buffer, 0);
                handler.onMessage(flyMessage);
                buffer.wrap(0, 0);
                bytes.readPosition(offset + size);
                return 1;
            }
            return 0;
        }
    }

    @Override
    public int peekOrPoll(final PeekableMessageLog.PeekPollHandler<? super M> handler) {
        try (final DocumentContext context = tailer.readingDocument()) {
            if (context.isData()) {
                final Bytes<?> bytes = context.wire().bytes();
                final int size = bytes.readInt();
                final int offset = (int) bytes.readPosition();
                final long addr = bytes.addressForRead(offset);
                buffer.wrap(addr, size);
                final M flyMessage = flyweight.init(buffer, 0);
                final Result result = handler.onMessage(flyMessage);
                buffer.wrap(0, 0);
                bytes.readPosition(offset + size);
                if (result == POLL) {
                    return 1;
                }
                context.rollbackOnClose();
                //NOTE: we have work done here, but if this work is the only
                //      bit performed in the duty cycle loop then the result
                //      in the next loop iteration will be the same, hence we
                //      better let the idle strategy do its job
            }
            return 0;
        }
    }

}
