/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.input;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.CommandType;

public interface Receiver {
    int LOOPBACK_SOURCE = -1;
    ReceivingContext receivingMessage(int source, long sequence);
    ReceivingContext receivingMessage(int source, long sequence, int type);
    void receiveMessage(int source, long sequence, DirectBuffer buffer, int offset, int length);
    void receiveMessage(int source, long sequence, int type, DirectBuffer buffer, int offset, int length);
    void receiveMessageWithoutPayload(int source, long sequence, int type);

    interface ReceivingContext extends AutoCloseable {
        MutableDirectBuffer buffer();
        void receive(int messageLength);
        void abort();
        boolean isClosed();

        @Override
        default void close() {
            if (!isClosed()) {
                abort();
            }
        }
    }

    interface Default extends Receiver {
        @Override
        default ReceivingContext receivingMessage(final int source, final long sequence) {
            return receivingMessage(source, sequence, CommandType.APPLICATION);
        }

        @Override
        default void receiveMessage(final int source, final long sequence, final DirectBuffer buffer, final int offset, final int length) {
            receiveMessage(source, sequence, CommandType.APPLICATION, buffer, offset, length);
        }

        @Override
        default void receiveMessage(final int source, final long sequence, final int type, final DirectBuffer buffer, final int offset, final int length) {
            try (final ReceivingContext context = receivingMessage(source, sequence, type)) {
                context.buffer().putBytes(0, buffer, offset, length);
                context.receive(length);
            }
        }

        @Override
        default void receiveMessageWithoutPayload(final int source, final long sequence, final int type) {
            try (final ReceivingContext context = receivingMessage(source, sequence, type)) {
                context.receive(0);
            }
        }
    }
}
