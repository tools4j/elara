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
package org.tools4j.elara.stream.ipc;

import org.agrona.IoUtil;
import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.ipc.impl.IpcBufferedSender;
import org.tools4j.elara.stream.ipc.impl.IpcDirectSender;
import org.tools4j.elara.stream.ipc.impl.IpcReceiver;
import org.tools4j.elara.stream.ipc.impl.IpcRetryOpenReceiver;
import org.tools4j.elara.stream.ipc.impl.IpcRetryOpenSender;
import org.tools4j.elara.stream.ipc.impl.IpcRingBuffer;
import org.tools4j.elara.stream.ipc.impl.RingBufferRetryOpener;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;

public enum Ipc {
    ;

    public static MessageSender newSender(final File file, final int length, final IpcConfiguration config) {
        return config.senderAllocationStrategy() == AllocationStrategy.FIXED ?
                new IpcDirectSender(file, length, config) :
                new IpcBufferedSender(file, length, config);
    }

    public static MessageSender openSender(final File file, final IpcConfiguration config) {
        final MappedByteBuffer mappedByteBuffer = IoUtil.mapExistingFile(file, READ_WRITE, file.getAbsolutePath());
        return config.senderAllocationStrategy() == AllocationStrategy.FIXED ?
                new IpcDirectSender(mappedByteBuffer, config) :
                new IpcBufferedSender(mappedByteBuffer, config);
    }

    public static MessageSender retryOpenSender(final File file, final IpcConfiguration config) {
        return new IpcRetryOpenSender(new RingBufferRetryOpener(file, config), config);
    }

    public static MessageReceiver newReceiver(final File file, final int length, final IpcConfiguration config) {
        return new IpcReceiver(file, length, config);
    }

    public static MessageReceiver openReceiver(final File file, final IpcConfiguration config) {
        return new IpcReceiver(IoUtil.mapExistingFile(file, READ_WRITE, file.getAbsolutePath()), config);
    }

    public static MessageReceiver retryOpenReceiver(final File file, final IpcConfiguration config) {
        return new IpcRetryOpenReceiver(new RingBufferRetryOpener(file, config), config);
    }

    public static SharedBuffer share(final ByteBuffer buffer, final IpcConfiguration config) {
        return share(buffer, config);
    }

    public static SharedBuffer share(final AtomicBuffer buffer, final IpcConfiguration config) {
        return share(buffer, config);
    }

    public static SharedBuffer share(final RingBuffer ringBuffer, final IpcConfiguration config) {
        return new IpcRingBuffer(ringBuffer, config);
    }

}
