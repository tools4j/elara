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
package org.tools4j.elara.stream;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.stream.MessageSender.SendingContext;

import static java.util.Objects.requireNonNull;

/**
 * Buffered sending context for instance used by {@link MessageSender.Buffered}.
 */
public class BufferingSendingContext implements SendingContext {
    private final MessageSender sender;
    private final MutableDirectBuffer buffer;
    private boolean closed = true;

    public BufferingSendingContext(final MessageSender sender, final MutableDirectBuffer buffer) {
        this.sender = requireNonNull(sender);
        this.buffer = requireNonNull(buffer);
    }

    protected BufferingSendingContext init() {
        if (!closed) {
            abort();
            throw new IllegalStateException("Sending context not closed");
        }
        closed = false;
        return this;
    }

    private MutableDirectBuffer unclosedBuffer() {
        if (closed) {
            throw new IllegalStateException("Sending context closed");
        }
        return buffer;
    }

    @Override
    public MutableDirectBuffer buffer() {
        return unclosedBuffer();
    }

    @Override
    public SendingResult send(final int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        final DirectBuffer buffer = unclosedBuffer();
        try {
            return sender.sendMessage(buffer, 0, length);
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
