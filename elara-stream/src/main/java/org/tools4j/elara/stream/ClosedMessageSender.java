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
package org.tools4j.elara.stream;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;

final class ClosedMessageSender implements MessageSender {

    private final ClosedSenderContext sendingContext = new ClosedSenderContext();

    @Override
    public SendingContext sendingMessage() {
        return sendingContext.init();
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        return SendingResult.CLOSED;
    }

    @Override
    public boolean isClosed() {
        return true;
    }

    @Override
    public void close() {
        //no-op
    }

    private static final class ClosedSenderContext implements SendingContext {

        private final MutableDirectBuffer buffer = new ExpandableDirectByteBuffer(1024);
        private boolean closed = true;

        ClosedSenderContext init() {
            if (!closed) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            closed = false;
            return this;
        }

        @Override
        public MutableDirectBuffer buffer() {
            if (closed) {
                throw new IllegalStateException("Sending context is closed");
            }
            return buffer;
        }

        @Override
        public SendingResult send(final int length) {
            if (closed) {
                throw new IllegalStateException("Sending context is closed");
            }
            closed = true;
            return SendingResult.CLOSED;
        }

        @Override
        public void abort() {
            closed = true;
        }

        @Override
        public boolean isClosed() {
            return closed;
        }

        @Override
        public void close() {
            abort();
        }
    }
}
