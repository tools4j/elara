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
package org.tools4j.elara.stream.ipc.impl;

import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.stream.MessageSender;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class IpcRetryOpenSender extends MessageSender.Buffered {

    private final Supplier<? extends RingBuffer> ringBufferSupplier;
    private RingBuffer ringBuffer;
    private boolean closed;

    public IpcRetryOpenSender(final Supplier<? extends RingBuffer> ringBufferSupplier) {
        this.ringBufferSupplier = requireNonNull(ringBufferSupplier);
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        RingBuffers.close(ringBuffer);
        if (ringBufferSupplier instanceof AutoCloseable) {
            CloseHelper.quietClose((AutoCloseable)ringBufferSupplier);
        }
        closed = true;
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        try {
            if (ringBuffer == null) {
                if (closed) {
                    return SendingResult.CLOSED;
                }
                ringBuffer = ringBufferSupplier.get();
            }
            if (ringBuffer != null) {
                if (RingBuffers.write(ringBuffer, buffer, offset, length)) {
                    return SendingResult.SENT;
                }
                return SendingResult.BACK_PRESSURED;
            }
            return SendingResult.DISCONNECTED;
        } catch (final Exception e) {
            //TODO log error
            return SendingResult.FAILED;
        }
    }

}
