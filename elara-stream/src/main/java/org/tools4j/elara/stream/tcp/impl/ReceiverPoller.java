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
package org.tools4j.elara.stream.tcp.impl;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class ReceiverPoller extends TcpPoller {

    private final Supplier<? extends RingBuffer> ringBufferFactory;
    private final DirectBuffer readBuffer = new UnsafeBuffer(0, 0);
    private final SelectionHandler chainedHandler = this::onSelectionKey;

    private SelectionHandler selectionHandler = SelectionHandler.NO_OP;
    private Handler messageHandler = message -> {};

    ReceiverPoller(final Supplier<? extends RingBuffer> ringBufferFactory) {
        this.ringBufferFactory = requireNonNull(ringBufferFactory);
    }

    int selectNow(final SelectionHandler selectionHandler, final Handler messageHandler) throws IOException {
        if (this.selectionHandler != selectionHandler) {
            this.selectionHandler = requireNonNull(selectionHandler);
        }
        if (this.messageHandler != messageHandler) {
            this.messageHandler = requireNonNull(messageHandler);
        }
        return selectNow(chainedHandler);
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        final int result = selectionHandler.onSelectionKey(key);
        if (result != SelectionHandler.OK) {
            return result;
        }
        if (!key.isReadable()) {
            return SelectionHandler.OK;
        }
        final RingBuffer ringBuffer = SelectionKeyAttachments.fetchOrAttach(key, ringBufferFactory);
        readFromChannel(key, ringBuffer);
        return handleMessages(ringBuffer);
    }

    private void readFromChannel(final SelectionKey key, final RingBuffer ringBuffer) throws IOException {
        final ByteBuffer buffer = ringBuffer.writeOrNull();
        if (buffer != null) {
            final SocketChannel channel = (SocketChannel) key.channel();
            ringBuffer.writeCommit(channel.read(buffer));
        }
    }

    private int handleMessages(final RingBuffer ringBuffer) {
        int readLength = ringBuffer.readLength();
        while (readLength >= Integer.BYTES) {
            final int messageLength = (int)ringBuffer.readUnsignedInt(false);
            readLength -= Integer.BYTES;
            if (readLength < messageLength) {
                if (ringBuffer.capacity() < messageLength + Integer.BYTES) {
                    ringBuffer.reset();
                    return SelectionHandler.MESSAGE_TOO_LARGE;
                }
                break;
            }
            ringBuffer.readCommit(Integer.BYTES);
            handleMessage(ringBuffer, messageLength);
            readLength = ringBuffer.readLength();
        }
        movePartialMessagesToStart(ringBuffer);
        return SelectionHandler.OK;
    }

    private void handleMessage(final RingBuffer ringBuffer, final int length) {
        ringBuffer.readWrap(readBuffer, length);
        try {
            messageHandler.onMessage(readBuffer);
        } finally {
            readBuffer.wrap(0, 0);
            ringBuffer.readCommit(length);
        }
    }

    private void movePartialMessagesToStart(final RingBuffer ringBuffer) {
        if (ringBuffer.readWrap(readBuffer)) {
            final int readLength = readBuffer.capacity();
            ringBuffer.readCommit(readLength);
            ringBuffer.write(readBuffer, 0, readLength);
            readBuffer.wrap(0, 0);
        }
    }

}