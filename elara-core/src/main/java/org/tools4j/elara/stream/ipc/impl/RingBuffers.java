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

import org.agrona.DirectBuffer;
import org.agrona.IoUtil;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.concurrent.ringbuffer.ManyToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.OneToOneRingBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.ipc.IpcConfiguration;

import java.io.File;
import java.nio.ByteBuffer;

import static org.agrona.concurrent.ringbuffer.RingBufferDescriptor.TRAILER_LENGTH;

enum RingBuffers {
    ;
    private static final int MSG_TYPE_ID = 1;

    static boolean write(final RingBuffer ringBuffer, final DirectBuffer buffer, final int offset, final int length) {
        if (ringBuffer.write(MSG_TYPE_ID, buffer, offset, length)) {
            return true;
        }
        if (length <= ringBuffer.maxMsgLength()) {
            return false;
        }
        throw new IllegalArgumentException("Message length " + length + " exceeds max ring buffer message length " +
                ringBuffer.maxMsgLength());
    }

    static boolean isClosed(final RingBuffer ringBuffer) {
        return ringBuffer.buffer().capacity() == 0;
    }

    static void close(final RingBuffer ringBuffer) {
        if (ringBuffer == null || isClosed(ringBuffer)) {
            return;
        }
        final ByteBuffer byteBuffer = ringBuffer.buffer().byteBuffer();
        if (byteBuffer != null) {
            IoUtil.unmap(byteBuffer);
        }
        ringBuffer.buffer().wrap(0, 0);
    }

    static RingBuffer newFileMapped(final File file, final int length, final IpcConfiguration config) {
        final File dir = file.getParentFile();
        if (dir != null && config.newFileCreateParentDirs()) {
            IoUtil.ensureDirectoryExists(dir, dir.getAbsolutePath());
        }
        if (config.newFileDeleteIfPresent()) {
            IoUtil.deleteIfExists(file);
        }
        return create(IoUtil.mapNewFile(file, length + TRAILER_LENGTH), config);
    }

    static RingBuffer create(final ByteBuffer buffer, final IpcConfiguration config) {
        return create(new UnsafeBuffer(buffer), config);
    }

    static RingBuffer create(final AtomicBuffer buffer, final IpcConfiguration config) {
        switch (config.senderCardinality()) {
            case ONE:
                return new OneToOneRingBuffer(buffer);
            case MANY:
                new ManyToOneRingBuffer(buffer);
            default:
                throw new IllegalArgumentException("Unsupported server cardinality: " + config.senderCardinality());
        }
    }
}