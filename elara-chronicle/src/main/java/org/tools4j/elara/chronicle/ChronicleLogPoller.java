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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import net.openhft.chronicle.queue.RollCycle;
import net.openhft.chronicle.queue.TailerDirection;
import net.openhft.chronicle.wire.DocumentContext;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.log.MessageLog;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.log.MessageLog.Handler;
import static org.tools4j.elara.log.MessageLog.Handler.Result;
import static org.tools4j.elara.log.MessageLog.Handler.Result.POLL;
import static org.tools4j.elara.log.MessageLog.Poller;

public class ChronicleLogPoller implements Poller {

    private final ExcerptTailer tailer;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    public ChronicleLogPoller(final ChronicleQueue queue) {
        this(queue.createTailer());
    }

    public ChronicleLogPoller(final String id, final ChronicleQueue queue) {
        this(queue.createTailer(id));
    }

    public ChronicleLogPoller(final ExcerptTailer tailer) {
        this.tailer = requireNonNull(tailer);
    }

    public ChronicleQueue queue() {
        return tailer.queue();
    }

    public RollCycle rollCycle() {
        return queue().rollCycle();
    }

    public int cycle() {
        return rollCycle().toCycle(tailer.index());
    }

    public long sequence() {
        return rollCycle().toSequenceNumber(tailer.index());
    }

    @Override
    public long entryId() {
        return tailer.index();
    }

    @Override
    public boolean moveTo(final long entryId) {
        final long curEntryId = tailer.index();
        if (tailer.moveToIndex(entryId)) {
            return true;
        }
        tailer.moveToIndex(curEntryId);
        return false;
    }

    @Override
    public ChronicleLogPoller moveToStart() {
        tailer.toStart();
        return this;
    }

    @Override
    public MessageLog.Poller moveToEnd() {
        tailer.toEnd();
        return this;
    }

    @Override
    public boolean moveToNext() {
        boolean present;
        do {
            final long index = tailer.index();
            try (final DocumentContext context = tailer.readingDocument()) {
                if (context.isData()) {
                    if (index != context.index()) {
                        context.rollbackOnClose();
                    }
                    return true;
                }
                present = context.isPresent();
            }
        } while (present);
        return false;
    }

    @Override
    public boolean moveToPrevious() {
        tailer.direction(TailerDirection.BACKWARD);
        boolean moved;
        try {
            moved = moveToNext();
        } finally {
            tailer.direction(TailerDirection.FORWARD);
            if (tailer.index() < tailer.queue().firstIndex()) {
                //weirdly it moves before start when going from end one back with a single entry in the queue
                tailer.toStart();
                moved = false;
            }
        }
        return moved;
    }

    @Override
    public int poll(final Handler handler) {
        try (DocumentContext context = tailer.readingDocument()) {
            if (context.isData()) {
                final Bytes<?> bytes = context.wire().bytes();
                final int size = bytes.readInt();
                final long offset = bytes.readPosition();
                final long addr = bytes.addressForRead(offset);
                buffer.wrap(addr, size);
                final Result result = handler.onMessage(buffer);
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

    @Override
    public void close() {
        tailer.close();
        buffer.wrap(0, 0);
    }

    @Override
    public String toString() {
        return "ChronicleLogPoller{" +
                "tailer=" + tailer +
                '}';
    }
}
