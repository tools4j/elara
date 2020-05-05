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
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.log.MessageLog;

import static java.util.Objects.requireNonNull;

public class ChronicleLogAppender implements MessageLog.Appender {

    private final ExcerptAppender appender;
    private final UnsafeBuffer bytesWrapper = new UnsafeBuffer(0, 0);
    private final AppendContext appendContext = new AppendContext();

    public ChronicleLogAppender(final ChronicleQueue queue) {
        this(queue.acquireAppender());
    }

    public ChronicleLogAppender(final ExcerptAppender appender) {
        this.appender = requireNonNull(appender);
    }

    @Override
    public void append(final DirectBuffer buffer, final int offset, final int length) {
        try (final DocumentContext context = appender.writingDocument()) {
            try {
                final int totalLength = length + 4;
                final Bytes<?> bytes = context.wire().bytes();
                bytes.ensureCapacity(totalLength);
                final long dstOffset = bytes.writePosition();
                final long addr = bytes.addressForWrite(dstOffset);
                bytesWrapper.wrap(addr, totalLength);
                bytesWrapper.putInt(0, length);
                bytesWrapper.putBytes(4, buffer, offset, length);
                bytesWrapper.wrap(0, 0);
                bytes.writePosition(bytes.writePosition() + totalLength);
            } catch (final Exception e) {
                context.rollbackOnClose();
                throw e;
            }
        }
    }

    @Override
    public MessageLog.AppendContext appending() {
        return appendContext.init(appender.writingDocument());
    }

    private static class AppendContext implements MessageLog.AppendContext {
        private final BytesDirectBuffer buffer = new BytesDirectBuffer();
        private DocumentContext context;

        AppendContext init(final DocumentContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Aborted unclosed append context");
            }
            final Bytes<?> bytes = context.wire().bytes();
            bytes.writeInt(0);//place holder for length
            this.buffer.wrapForWriting(bytes);
            this.context = context;
            return this;
        }

        @Override
        public MutableDirectBuffer buffer() {
            return buffer;
        }

        @Override
        public void commit(final int length) {
            if (context == null) {
                throw new IllegalStateException("Append context is closed");
            }
            try (final DocumentContext dc = context) {
                try {
                    buffer.unwrap();
                    final Bytes<?> bytes = context.wire().bytes();
                    bytes.ensureCapacity(length);
                    bytes.writePosition(bytes.writePosition() - 4);
                    bytes.writeInt(length);
                    bytes.writePosition(bytes.writePosition() + length);
                } catch (final Exception e) {
                    dc.rollbackOnClose();
                    throw e;
                } finally {
                    context = null;
                }
            }
        }

        @Override
        public void abort() {
            if (context == null) {
                throw new IllegalStateException("Append context is closed");
            }
            buffer.unwrap();
            context.rollbackOnClose();
            context.close();
            context = null;
        }

        @Override
        public boolean isClosed() {
            return context == null;
        }
    }
}
