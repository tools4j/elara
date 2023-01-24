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
package org.tools4j.elara.stream.ipc.impl;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.ipc.IpcConfiguration;

import java.io.File;
import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public class IpcDirectSender implements MessageSender.Direct {

    private final int allocationSize;
    private final RingBuffer ringBuffer;
    private final IpcSendingContext context = new IpcSendingContext();

    public IpcDirectSender(final File file, final int length, final IpcConfiguration config) {
        this(RingBuffers.newFileMapped(file, length, config), config);
    }

    public IpcDirectSender(final ByteBuffer buffer, final IpcConfiguration config) {
        this(RingBuffers.create(buffer, config), config);
    }

    public IpcDirectSender(final RingBuffer ringBuffer, final IpcConfiguration config) {
        this.allocationSize = config.senderInitialBufferSize();
        this.ringBuffer = requireNonNull(ringBuffer);
    }

    @Override
    public SendingContext sendingMessage() {
        return context.init();
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
        return "IpcDirectSender";
    }

    private final class IpcSendingContext implements MessageSender.SendingContext {
        final MutableDirectBuffer direct = new UnsafeBuffer(0, 0);
        MutableDirectBuffer buffered;
        MutableDirectBuffer buffer;

        IpcSendingContext init() {
            if (buffer != null) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            final int index = ringBuffer.tryClaim(RingBuffers.LENGTH_PREFIXED_MSG_TYPE_ID, allocationSize);
            if (index > 0) {
                direct.wrap(ringBuffer.buffer(), index + Integer.BYTES, allocationSize - Integer.BYTES);
                buffer = direct;
            } else {
                if (buffered == null) {
                    //NOTE: subtracting 4 bytes is not strictly required, but we want to keep max capacity consistent
                    //      with the direct case
                    buffered = new UnsafeBuffer(ByteBuffer.allocateDirect(allocationSize - Integer.BYTES));
                }
                buffer = buffered;
            }
            return this;
        }

        private MutableDirectBuffer unclosedBuffer() {
            final MutableDirectBuffer buffer = this.buffer;
            if (buffer != null) {
                return buffer;
            }
            throw new IllegalStateException("Sending context closed");
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
            final MutableDirectBuffer buffer = unclosedBuffer();
            try {
                if (buffer == direct) {
                    final int index = direct.wrapAdjustment() - Integer.BYTES;
                    ringBuffer.buffer().putInt(index, length);
                    ringBuffer.commit(index);
                    return SendingResult.SENT;
                }
                if (RingBuffers.write(ringBuffer, buffer, 0, length)) {
                    return SendingResult.SENT;
                }
                return SendingResult.BACK_PRESSURED;
            } finally {
                this.direct.wrap(0, 0);
                this.buffer = null;
            }
        }

        @Override
        public void abort() {
            if (buffer != null) {
                direct.wrap(0, 0);
                buffer = null;
            }
        }

        @Override
        public boolean isClosed() {
            return buffer == null;
        }
    }
}
