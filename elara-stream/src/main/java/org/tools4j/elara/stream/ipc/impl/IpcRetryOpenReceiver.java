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
package org.tools4j.elara.stream.ipc.impl;

import org.agrona.CloseHelper;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.ipc.IpcConfig;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class IpcRetryOpenReceiver implements MessageReceiver {
    private final Supplier<? extends RingBuffer> ringBufferSupplier;
    private final int maxMessagesPerPoll;
    private final IpcMessageHandler messageHandler = new IpcMessageHandler();

    private RingBuffer ringBuffer;
    private boolean closed;

    public IpcRetryOpenReceiver(final Supplier<? extends RingBuffer> ringBufferSupplier, final IpcConfig config) {
        this.ringBufferSupplier = requireNonNull(ringBufferSupplier);
        this.maxMessagesPerPoll = Math.max(1, config.maxMessagesReceivedPerPoll());
    }

    @Override
    public int poll(final Handler handler) {
        requireNonNull(handler);
        if (ringBuffer == null) {
            if (closed) {
                return 0;
            }
            ringBuffer = ringBufferSupplier.get();
            if (ringBuffer == null) {
                return 0;
            }
        }
        return ringBuffer.read(messageHandler.init(handler), maxMessagesPerPoll);
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
    public String toString() {
        return "IpcRetryOpenReceiver";
    }
}
