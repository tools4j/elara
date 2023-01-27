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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.stream.nio.SelectionHandler.BACK_PRESSURE;
import static org.tools4j.elara.stream.nio.SelectionHandler.MESSAGE_TOO_LARGE;
import static org.tools4j.elara.stream.nio.SelectionHandler.OK;

public class WriteSelectionHandler {

    private final Supplier<? extends RingBuffer> ringBufferFactory;
    private final SelectionHandler baseHandler;
    private final SelectionHandler writeHandler = this::onSelectionKey;
    private final DirectBuffer frame = new UnsafeBuffer(0, 0);

    public WriteSelectionHandler(final Supplier<? extends RingBuffer> ringBufferFactory) {
        this(ringBufferFactory, SelectionHandler.NO_OP);
    }

    public WriteSelectionHandler(final Supplier<? extends RingBuffer> ringBufferFactory,
                                 final SelectionHandler baseHandler) {
        this.ringBufferFactory = requireNonNull(ringBufferFactory);
        this.baseHandler = requireNonNull(baseHandler);
    }

    public SelectionHandler init(final DirectBuffer buffer, final int offset, final int length) {
        frame.wrap(buffer, offset, length);
        return writeHandler;
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        int result = baseHandler.onSelectionKey(key);
        if (result != OK) {
            return result;
        }
        if (!key.isWritable()) {
            return BACK_PRESSURE;
        }
        final RingBuffer ringBuffer = SelectionKeyAttachments.fetchOrAttach(key, ringBufferFactory);
        if (frame.capacity() > ringBuffer.capacity()) {
            return MESSAGE_TOO_LARGE;
        }
        if (!writeRemaining(key.channel(), ringBuffer)) {
            return BACK_PRESSURE;
        }
        writeMessage(key.channel(), ringBuffer);
        return OK;
    }

    private boolean writeRemaining(final Channel channel, final RingBuffer ringBuffer) throws IOException {
        ByteBuffer buffer = ringBuffer.readOrNull();
        if (buffer == null) {
            //no remaining
            return true;
        }
        final int remaining = buffer.remaining();
        final int written = writeToChannel(channel, buffer);
        ringBuffer.readCommit(written);
        //if we have written all, check if we have more to write due to wrap at the end of the ring buffer
        if (written == remaining && (buffer = ringBuffer.readOrNull()) != null) {
            ringBuffer.readCommit(writeToChannel(channel, buffer));
        }
        return ringBuffer.readLength() == 0;
    }

    private int writeMessage(final Channel channel, final RingBuffer ringBuffer) throws IOException {
        if (frame.byteBuffer() != null) {
            return writeMessageDirect(channel, ringBuffer) ? OK : BACK_PRESSURE;
        }
        ringBuffer.write(frame, 0, frame.capacity());
        if (writeRemaining(channel, ringBuffer)) {
            return OK;
        }
        if (ringBuffer.readLength() == frame.capacity()) {
            //nothing was sent, let's clear the ring buffer and report backpressure
            ringBuffer.reset();
            return BACK_PRESSURE;
        }
        return OK;
    }

    //PRECONDITION: frame.frame().byteBuffer() != null
    private boolean writeMessageDirect(final Channel channel, final RingBuffer ringBuffer) throws IOException {
        final ByteBuffer buffer = frame.byteBuffer();
        final int length = frame.capacity();
        final int adjustment = frame.wrapAdjustment();
        buffer.limit(adjustment + length);
        buffer.position(adjustment);
        final int written = writeToChannel(channel, buffer);
        if (written == 0) {
            //nothing was sent, let's report backpressure
            return false;
        }
        if (written < length) {
            assert ringBuffer.write(frame, written, length - written);
        }
        return true;
    }

    protected int writeToChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
        return ((ByteChannel)channel).write(buffer);
    }

}