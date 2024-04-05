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

import org.agrona.DirectBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.ipc.IpcConfig;

import java.io.File;
import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public class IpcBufferedSender extends MessageSender.Buffered {

    private final RingBuffer ringBuffer;

    public IpcBufferedSender(final File file, final int length, final IpcConfig config) {
        this(RingBuffers.newFileMapped(file, length, config), config);
    }

    public IpcBufferedSender(final ByteBuffer buffer, final IpcConfig config) {
        this(RingBuffers.create(buffer, config), config);
    }

    public IpcBufferedSender(final RingBuffer ringBuffer, final IpcConfig config) {
        super(config.senderInitialBufferSize());
        this.ringBuffer = requireNonNull(ringBuffer);
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        if (RingBuffers.write(ringBuffer, buffer, offset, length)) {
            return SendingResult.SENT;
        }
        return isClosed() ? SendingResult.CLOSED : SendingResult.BACK_PRESSURED;
    }

    @Override
    public boolean isClosed() {
        return RingBuffers.isClosed(ringBuffer);
    }

    @Override
    public void close() {
        RingBuffers.close(ringBuffer);
    }

    @Override
    public String toString() {
        return "IpcBufferedSender";
    }
}
