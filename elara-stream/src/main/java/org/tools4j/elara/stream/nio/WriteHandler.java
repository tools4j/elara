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
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.stream.nio.SelectionHandler.BACK_PRESSURE;
import static org.tools4j.elara.stream.nio.SelectionHandler.OK;

public class WriteHandler {

    private final ChannelBufferSupplier channelBufferSupplier;
    private final SelectionHandler baseHandler;
    private final SelectionHandler writeHandler = this::onSelectionKey;
    private final MutableDirectBuffer writeBuffer = new UnsafeBuffer(0, 0);
    private DirectBuffer frame;

    public WriteHandler(final ChannelBufferSupplier channelBufferSupplier) {
        this(channelBufferSupplier, SelectionHandler.NO_OP);
    }

    public WriteHandler(final ChannelBufferSupplier channelBufferSupplier,
                        final SelectionHandler baseHandler) {
        this.channelBufferSupplier = requireNonNull(channelBufferSupplier);
        this.baseHandler = requireNonNull(baseHandler);
    }

    private void init(final NioFrame frame) {
        final DirectBuffer frameBuffer = frame.frame();
        if (this.frame != frameBuffer) {
            this.frame = requireNonNull(frameBuffer);
        }
    }
    public SelectionHandler initForSelection(final NioFrame frame) {
        init(frame);
        return writeHandler;
    }

    public int send(final Channel channel, final NioFrame frame) throws IOException{
        init(frame);
        return sendToChannel(channel);
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        int result = baseHandler.onSelectionKey(key);
        if (result != OK) {
            return result;
        }
        if (!key.isWritable()) {
            return BACK_PRESSURE;
        }
        return sendToChannel(key.channel());
    }

    private int sendToChannel(final Channel channel) throws IOException{
        final ByteBuffer byteBuffer = channelBufferSupplier.bufferFor(channel);
        if (!writeRemaining(channel, byteBuffer)) {
            return BACK_PRESSURE;
        }
        writeMessage(channel, byteBuffer);
        return OK;
    }

    private boolean writeRemaining(final Channel channel, final ByteBuffer buffer) throws IOException {
        final int available = buffer.position();
        if (available == 0) {
            //no remaining
            return true;
        }
        buffer.flip();
        writeToChannel(channel, buffer);
        final int remaining = buffer.remaining();
        if (remaining == 0) {
            buffer.clear();
            return true;
        }
        if (buffer.position() > 0) {
            writeBuffer.wrap(buffer, 0, buffer.capacity());
            writeBuffer.putBytes(0, buffer, buffer.position(), remaining);
        }
        buffer.limit(buffer.capacity());
        buffer.position(remaining);
        return false;
    }

    private int writeMessage(final Channel channel, final ByteBuffer buffer) throws IOException {
        buffer.clear();
        if (frame.byteBuffer() != null) {
            return writeMessageDirect(channel, buffer) ? OK : BACK_PRESSURE;
        }
        frame.getBytes(0, buffer, frame.capacity());
        if (writeRemaining(channel, buffer)) {
            return OK;
        }
        if (buffer.remaining() == frame.capacity()) {
            //nothing was sent, let's clear the ring buffer and report backpressure
            buffer.clear();
            return BACK_PRESSURE;
        }
        return OK;
    }

    //PRECONDITION: frame.frame().byteBuffer() != null
    private boolean writeMessageDirect(final Channel channel, final ByteBuffer buffer) throws IOException {
        final ByteBuffer frameBuffer = frame.byteBuffer();
        final int length = frame.capacity();
        final int adjustment = frame.wrapAdjustment();
        frameBuffer.limit(adjustment + length);
        frameBuffer.position(adjustment);
        int written = writeToChannel(channel, frameBuffer);
        if (written == 0) {
            //nothing was sent, let's report backpressure
            return false;
        }
        if (written < length) {
            frame.getBytes(written, buffer, length - written);
        }
        return true;
    }

    protected int writeToChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
        return ((ByteChannel)channel).write(buffer);
    }

}