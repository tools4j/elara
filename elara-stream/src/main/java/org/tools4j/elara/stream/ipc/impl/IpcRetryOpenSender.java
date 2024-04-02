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
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.BufferingSendingContext;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.ipc.IpcConfiguration;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.stream.ipc.AllocationStrategy.FIXED;

public class IpcRetryOpenSender implements MessageSender {

    private final Supplier<? extends RingBuffer> ringBufferSupplier;
    private final BiFunction<? super RingBuffer, ? super IpcConfiguration, ? extends MessageSender> senderFactory;
    private final IpcConfiguration config;
    private MessageSender sender;
    private BufferedContext bufferedContext;
    private boolean closed;

    public IpcRetryOpenSender(final Supplier<? extends RingBuffer> ringBufferSupplier,
                              final IpcConfiguration config) {
        this.ringBufferSupplier = requireNonNull(ringBufferSupplier);
        this.senderFactory = config.senderAllocationStrategy() == FIXED ? IpcDirectSender::new : IpcBufferedSender::new;
        this.config = requireNonNull(config);
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
        if (sender != null) {
            sender.close();
            sender = null;
        }
        closed = true;
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        try {
            if (sender == null) {
                if (closed) {
                    return SendingResult.CLOSED;
                }
                sender = tryOpenSender();
            }
            if (sender != null) {
                return sender.sendMessage(buffer, offset, length);
            }
            return SendingResult.DISCONNECTED;
        } catch (final Exception e) {
            //TODO log error
            return SendingResult.FAILED;
        }
    }

    @Override
    public SendingContext sendingMessage() {
        if (sender == null) {
            if (closed) {
                return CLOSED.sendingMessage();
            }
            sender = tryOpenSender();
        }
        if (sender != null) {
            return sender.sendingMessage();
        }
        if (bufferedContext == null) {
            bufferedContext = new BufferedContext();
        }
        return bufferedContext.init();
    }

    private MessageSender tryOpenSender() {
        final RingBuffer ringBuffer = ringBufferSupplier.get();
        if (ringBuffer != null) {
            return senderFactory.apply(ringBuffer, config);
        }
        return null;
    }

    @Override
    public String toString() {
        return "IpcRetryOpenSender";
    }

    private final class BufferedContext extends BufferingSendingContext {
        BufferedContext() {
            super(IpcRetryOpenSender.this, new ExpandableDirectByteBuffer(config.senderInitialBufferSize()));
        }

        @Override
        protected BufferingSendingContext init() {
            return super.init();
        }
    }
}
