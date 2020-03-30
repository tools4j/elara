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
import net.openhft.chronicle.queue.ExcerptAppender;
import net.openhft.chronicle.wire.DocumentContext;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.Writable;

import static java.util.Objects.requireNonNull;

public class ChronicleLogAppender<M extends Writable> implements MessageLog.Appender<M> {

    private static final int MAX_MESSAGE_SIZE = 1 << 16;//TODO make this configurable

    private final ExcerptAppender appender;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    public ChronicleLogAppender(final ChronicleQueue queue) {
        this(queue.acquireAppender());
    }

    public ChronicleLogAppender(final ExcerptAppender appender) {
        this.appender = requireNonNull(appender);
    }

    @Override
    public void append(final M message) {
        try (final DocumentContext context = appender.writingDocument()) {
            final Bytes<?> bytes = context.wire().bytes();
            bytes.ensureCapacity(MAX_MESSAGE_SIZE);
            final int offset = (int)bytes.writePosition();
            final long addr = bytes.addressForWrite(offset);
            final int capacity = (int)Math.min(MAX_MESSAGE_SIZE, bytes.writeRemaining());
            buffer.wrap(addr, capacity);
            final int written = message.writeTo(buffer, 4);
            buffer.putInt(0, written);
            buffer.wrap(0, 0);
            bytes.writePosition(offset + written + 4);
        }
    }

}
