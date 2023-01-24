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
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class SenderPoller extends NioPoller {

    private final Supplier<? extends RingBuffer> ringBufferFactory;
    private final DirectBuffer readBuffer = new UnsafeBuffer(0, 0);
    private final SelectionHandler chainedHandler = this::onSelectionKey;

    private SelectionHandler selectionHandler = SelectionHandler.NO_OP;

    public SenderPoller(final Supplier<? extends RingBuffer> ringBufferFactory) {
        this.ringBufferFactory = requireNonNull(ringBufferFactory);
    }

    public int selectNow(final SelectionHandler selectionHandler,
                         final DirectBuffer buffer, final int offset, final int length) throws IOException {
        if (this.selectionHandler != selectionHandler) {
            this.selectionHandler = requireNonNull(selectionHandler);
        }
        readBuffer.wrap(buffer, offset, length);
        return selectNow(chainedHandler);
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        int result = selectionHandler.onSelectionKey(key);
        if (result != SelectionHandler.OK) {
            return result;
        }
        if (!key.isWritable()) {
            return SelectionHandler.OK;
        }
        final ByteChannel channel = (ByteChannel) key.channel();
        final RingBuffer ringBuffer = SelectionKeyAttachments.fetchOrAttach(key, ringBufferFactory);
        result = checkCacheMessageLengthAndPayload(ringBuffer);
        if (result != SelectionHandler.OK) {
            if (result != SelectionHandler.BACK_PRESSURE || !writeRemaining(channel, ringBuffer)) {
                return result;
            }
        }
        result = checkCacheMessageLengthAndPayload(ringBuffer);
        if (result != SelectionHandler.OK) {
            return result;
        }
        cacheMessageLengthAndPayload(ringBuffer);
        writeRemaining(channel, ringBuffer);
        return SelectionHandler.OK;
    }

    private boolean writeRemaining(final ByteChannel channel, final RingBuffer ringBuffer) throws IOException {
        ByteBuffer buffer = ringBuffer.readOrNull();
        if (buffer == null) {
            //no remaining
            return true;
        }
        final int remaining = buffer.remaining();
        final int written = channel.write(buffer);
        ringBuffer.readCommit(written);
        //if we have written all, check if we have more to write due to wrap at the end of the ring buffer
        if (written == remaining && (buffer = ringBuffer.readOrNull()) != null) {
            ringBuffer.readCommit(channel.write(buffer));
        }
        return ringBuffer.readLength() == 0;
    }

    private int checkCacheMessageLengthAndPayload(final RingBuffer ringBuffer) {
        final int payloadLength = readBuffer.capacity();
        final int totalLength = Integer.BYTES + payloadLength;
        if (totalLength <= ringBuffer.writeLength()) {
            return SelectionHandler.OK;
        }
        return totalLength <= ringBuffer.capacity() ? SelectionHandler.BACK_PRESSURE : SelectionHandler.MESSAGE_TOO_LARGE;
    }

    private boolean writeMessageLength(final ByteChannel channel, final RingBuffer ringBuffer) throws IOException {
        cacheMessageLength(ringBuffer);
        return writeRemaining(channel, ringBuffer);
    }

    private void writeOrCacheMessagePayload(final ByteChannel channel, final RingBuffer ringBuffer) throws IOException {
        if (!writeMessagePayloadDirect(channel, ringBuffer)) {
            cacheMessagePayload(ringBuffer);
            writeRemaining(channel, ringBuffer);
        }
    }

    private boolean writeMessagePayloadDirect(final ByteChannel channel, final RingBuffer ringBuffer) throws IOException {
        final ByteBuffer buffer = readBuffer.byteBuffer();
        if (buffer == null) {
            return false;
        }
        final int length = readBuffer.capacity();
        final int adjustment = readBuffer.wrapAdjustment();
        buffer.limit(adjustment + length);
        buffer.position(adjustment);
        final int written = channel.write(buffer);
        if (written < length) {
            cacheMessagePayload(ringBuffer, written, length - written);
        }
        return true;
    }

    private void cacheMessageLength(final RingBuffer ringBuffer) throws IOException {
        final int length = readBuffer.capacity();
        if (ringBuffer.writeUnsignedInt(length)) {
            return;
        }
        if (ringBuffer.writeUnsignedByte((byte)(length >>> 24)) &&
                ringBuffer.writeUnsignedByte((byte)(length >>> 16)) &&
                ringBuffer.writeUnsignedByte((byte)(length >>> 8)) &&
                ringBuffer.writeUnsignedByte((byte)length)) {
            return;
        }
        //should not get here since we checked buffer capacity before attempting to write to it
        throw new IOException("Failed to write message length: " + length);
    }

    private void cacheMessageLengthAndPayload(final RingBuffer ringBuffer) throws IOException {
        cacheMessageLength(ringBuffer);
        cacheMessagePayload(ringBuffer);
    }

    private void cacheMessagePayload(final RingBuffer ringBuffer) throws IOException {
        cacheMessagePayload(ringBuffer, 0, readBuffer.capacity());
    }

    private void cacheMessagePayload(final RingBuffer ringBuffer, final int offset, final int length) throws IOException {
        int readOffset = offset;
        int readRemaining = length;
        int writeLength = ringBuffer.writeLength();
        while (readRemaining > 0 && writeLength > 0) {
            final int part = Math.min(readRemaining, writeLength);
            if (!ringBuffer.write(readBuffer, readOffset, part)) {
                break;
            }
            readOffset += part;
            readRemaining -= part;
            writeLength = ringBuffer.writeLength();
        }
        if (readRemaining == 0) {
            return;
        }
        //should not get here since we checked buffer capacity before attempting to write to it
        throw new IOException("Failed to write message payload: " + length);
    }

}