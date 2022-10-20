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

import org.agrona.concurrent.AtomicBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.ipc.IpcConfiguration;
import org.tools4j.elara.stream.ipc.SharedBuffer;

import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

public class IpcRingBuffer implements SharedBuffer {

    private final RingBuffer ringBuffer;
    private final IpcConfiguration config;
    private final ThreadLocal<MessageSender> senderThreadLocal;
    private MessageSender sender;
    private MessageReceiver receiver;

    public IpcRingBuffer(final ByteBuffer buffer, final IpcConfiguration config) {
        this(RingBuffers.create(buffer, config), config);
    }

    public IpcRingBuffer(final AtomicBuffer buffer, final IpcConfiguration config) {
        this(RingBuffers.create(buffer, config), config);
    }

    public IpcRingBuffer(final RingBuffer ringBuffer, final IpcConfiguration config) {
        this.ringBuffer = requireNonNull(ringBuffer);
        this.config = requireNonNull(config);
        this.senderThreadLocal = ThreadLocal.withInitial(() -> new IpcSender(ringBuffer, config));
    }

    @Override
    public MessageSender sender() {
        if (sender == null) {
            if (isClosed()) {
                return MessageSender.CLOSED;
            }
            sender = new IpcSender(ringBuffer, config);
        }
        return sender;
    }

    @Override
    public MessageSender senderForCurrentThread() {
        return isClosed() ? MessageSender.CLOSED : senderThreadLocal.get();
    }

    @Override
    public MessageReceiver receiver() {
        if (receiver == null) {
            if (isClosed()) {
                return MessageReceiver.CLOSED;
            }
            receiver = new IpcReceiver(ringBuffer, config);
        }
        return receiver;
    }

    @Override
    public boolean isMapped() {
        return !isClosed();
    }

    @Override
    public boolean isClosed() {
        return RingBuffers.isClosed(ringBuffer);
    }

    @Override
    public void close() {
        RingBuffers.close(ringBuffer);
        sender = null;
        receiver = null;
    }

}
