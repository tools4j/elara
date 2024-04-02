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
package org.tools4j.elara.store;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;

import static java.util.Objects.requireNonNull;

/**
 * Provides default implementation for stores without direct appending support;  messages are encoded into a reusable
 * buffer if necessary.
 */
abstract public class BufferingAppender implements Appender {

    private final BufferingContext context;

    /**
     * Constructor with initial buffer size
     * @param initialBufferSize the initial capacity of the buffer used to code to
     */
    public BufferingAppender(final int initialBufferSize) {
        this(new ExpandableDirectByteBuffer(initialBufferSize));
    }

    /**
     * Constructor with buffer used for when encoding via {@link #appending()}.
     * @param buffer the buffer used to code to
     */
    public BufferingAppender(final MutableDirectBuffer buffer) {
        this.context = new BufferingContext(buffer);
    }

    @Override
    public AppendingContext appending() {
        return context.init();
    }

    private final class BufferingContext implements AppendingContext {
        private final MutableDirectBuffer buffer;
        private boolean closed = true;

        public BufferingContext(final MutableDirectBuffer buffer) {
            this.buffer = requireNonNull(buffer);
        }

        BufferingContext init() {
            if (!closed) {
                abort();
                throw new IllegalStateException("Appending context not closed");
            }
            closed = false;
            return this;
        }

        MutableDirectBuffer unclosedBuffer() {
            if (closed) {
                throw new IllegalStateException("Appending context closed");
            }
            return buffer;
        }

        @Override
        public MutableDirectBuffer buffer() {
            return unclosedBuffer();
        }

        @Override
        public void commit(final int length) {
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            final DirectBuffer buffer = unclosedBuffer();
            try {
                append(buffer, 0, length);
            } finally {
                closed = true;
            }
        }

        @Override
        public void abort() {
            if (!closed) {
                closed = true;
            }
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
    }
}
