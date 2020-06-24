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
package org.tools4j.elara.samples.replication;

import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.input.Receiver.ReceivingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.replication.Channel.NULL_VALUE;

public class BufferInput implements Input {

    private final int source;
    private final Buffer buffer;
    private long sequence;

    public BufferInput(final int source, final Buffer buffer) {
        this.source = source;
        this.buffer = requireNonNull(buffer);
    }

    public int source() {
        return source;
    }

    public Buffer buffer() {
        return buffer;
    }

    @Override
    public Poller poller() {
        return new BufferPoller();
    }

    private final class BufferPoller implements Poller {
        @Override
        public int poll(final Receiver receiver) {
            final long value = buffer.consume();
            if (value == NULL_VALUE) {
                return 0;
            }
            sequence++;
            try (final ReceivingContext context = receiver.receivingMessage(source, sequence)) {
                context.buffer().putLong(0, value);
                context.receive(Long.BYTES);
            }
            return 1;
        }
    }
}
