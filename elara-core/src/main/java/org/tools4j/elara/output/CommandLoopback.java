/**
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
package org.tools4j.elara.output;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.CommandType;

public interface CommandLoopback {
    EnqueuingContext enqueuingCommand();
    EnqueuingContext enqueuingCommand(int type);

    void enqueueCommand(DirectBuffer command, int offset, int length);
    void enqueueCommand(int type, DirectBuffer command, int offset, int length);
    void enqueueCommandWithoutPayload(int type);

    interface EnqueuingContext extends AutoCloseable {
        MutableDirectBuffer buffer();
        void enqueue(int length);
        void abort();
        boolean isClosed();

        @Override
        default void close() {
            if (!isClosed()) {
                abort();
            }
        }
    }

    interface Default extends CommandLoopback {
        @Override
        default EnqueuingContext enqueuingCommand() {
            return enqueuingCommand(CommandType.APPLICATION);
        }

        @Override
        default void enqueueCommand(final DirectBuffer command, final int offset, final int length) {
            enqueueCommand(CommandType.APPLICATION, command, offset, length);
        }

        @Override
        default void enqueueCommand(final int type, final DirectBuffer command, final int offset, final int length) {
            try (final EnqueuingContext context = enqueuingCommand(type)) {
                context.buffer().putBytes(0, command, offset, length);
                context.enqueue(length);
            }
        }

        @Override
        default void enqueueCommandWithoutPayload(final int type) {
            try (final EnqueuingContext context = enqueuingCommand(type)) {
                context.enqueue(0);
            }
        }
    }
}
