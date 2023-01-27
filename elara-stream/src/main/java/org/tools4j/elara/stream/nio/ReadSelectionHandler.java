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
package org.tools4j.elara.stream.nio;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.stream.nio.SelectionHandler.MESSAGE_TOO_LARGE;
import static org.tools4j.elara.stream.nio.SelectionHandler.OK;

public class ReadSelectionHandler {

    private final Supplier<? extends RingBuffer> ringBufferFactory;
    private final SelectionHandler baseHandler;
    private final SelectionHandler readHandler = this::onSelectionKey;
    private final DirectBuffer readBuffer = new UnsafeBuffer(0, 0);

    private NioHeader header;
    private Handler messageHandler = message -> {};

    public ReadSelectionHandler(final Supplier<? extends RingBuffer> ringBufferFactory) {
        this(ringBufferFactory, SelectionHandler.NO_OP);
    }

    public ReadSelectionHandler(final Supplier<? extends RingBuffer> ringBufferFactory,
                                final SelectionHandler baseHandler) {
        this.ringBufferFactory = requireNonNull(ringBufferFactory);
        this.baseHandler = requireNonNull(baseHandler);
    }

    public SelectionHandler init(final NioHeader header, final Handler messageHandler) {
        if (this.header != header) {
            this.header = requireNonNull(header);
        }
        if (this.messageHandler != messageHandler) {
            this.messageHandler = requireNonNull(messageHandler);
        }
        return readHandler;
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        final int result = baseHandler.onSelectionKey(key);
        if (result != OK) {
            return result;
        }
        if (!key.isReadable()) {
            return OK;
        }
        final RingBuffer ringBuffer = SelectionKeyAttachments.fetchOrAttach(key, ringBufferFactory);
        readFromChannel(key, ringBuffer);
        return handleMessages(ringBuffer);
    }

    private void readFromChannel(final SelectionKey key, final RingBuffer ringBuffer) throws IOException {
        final ByteBuffer buffer = ringBuffer.writeOrNull();
        if (buffer != null) {
            ringBuffer.writeCommit(readFromChannel(key.channel(), buffer));
        }
    }

    protected int readFromChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
        return ((ByteChannel)channel).read(buffer);
    }

    private int handleMessages(final RingBuffer ringBuffer) {
        while (ringBuffer.readWrap(header.buffer(), header.headerLength())) {
            final int payloadLength = header.payloadLength();
            final int remaining = ringBuffer.readLength() - header.headerLength();
            if (remaining < payloadLength) {
                if (ringBuffer.capacity() < payloadLength + header.headerLength()) {
                    ringBuffer.reset();
                    return MESSAGE_TOO_LARGE;
                }
                break;
            }
            handleMessage(header, ringBuffer, payloadLength);
        }
        movePartialMessagesToStart(ringBuffer);
        return OK;
    }

    //PRECONDITION: header is wrapped
    private void handleMessage(final NioHeader header, final RingBuffer ringBuffer, final int payloadLength) {
        ringBuffer.readCommit(header.headerLength());
        ringBuffer.readWrap(readBuffer, payloadLength);
        try {
            handleMessage(header, readBuffer);
        } finally {
            ringBuffer.readCommit(payloadLength);
            readBuffer.wrap(0, 0);
            header.buffer().wrap(0, 0);
        }
    }

    protected void handleMessage(final NioHeader header, final DirectBuffer payload) {
        messageHandler.onMessage(payload);
    }

    private void movePartialMessagesToStart(final RingBuffer ringBuffer) {
        if (ringBuffer.writeOffset() < ringBuffer.readOffset() && ringBuffer.readWrap(readBuffer)) {
            final int readLength = readBuffer.capacity();
            ringBuffer.readCommit(readLength);
            ringBuffer.write(readBuffer, 0, readLength);
            readBuffer.wrap(0, 0);
        }
    }

}