
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

import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;

final class RingBufferImpl implements RingBuffer {

    public static final int ALIGNMENT = 64;

    private final MutableDirectBuffer buffer;
    private int readOffset;
    private int writeOffset;

    RingBufferImpl(final int bufferSize) {
        if (bufferSize < ALIGNMENT) {
            throw new IllegalArgumentException("Buffer size must be at least " + ALIGNMENT);
        }
        this.buffer = new UnsafeBuffer(BufferUtil.allocateDirectAligned(bufferSize, ALIGNMENT));
    }

    @Override
    public long readUnsignedInt(final boolean commit) {
        final int readLength = readLength();
        if (readLength >= Integer.BYTES) {
            final long value = 0xffffffffL & buffer.getInt(readOffset);
            if (commit) {
                readCommit0(Integer.BYTES);
            }
            return value;
        }
        return -1;
    }

    public int read(final MutableDirectBuffer target, final int targetOffset, final int maxLength) {
        final int readLength = readLength();
        if (readLength > 0) {
            final int length = Math.min(readLength, maxLength);
            buffer.getBytes(readOffset, target, targetOffset, length);
            readCommit0(length);
            return length;
        }
        return 0;
    }

    @Override
    public int read(final ByteBuffer target, final int targetOffset, final int maxLength) {
        final int readLength = readLength();
        if (readLength > 0) {
            final int length = Math.min(readLength, maxLength);
            buffer.getBytes(readOffset, target, targetOffset, length);
            readCommit0(length);
            return length;
        }
        return 0;
    }

    @Override
    public boolean readWrap(final DirectBuffer wrap) {
        return readWrap0(wrap, readLength());
    }

    @Override
    public boolean readWrap(final DirectBuffer wrap, final int maxLength) {
        return readWrap0(wrap, Math.min(readLength(), maxLength));
    }

    private boolean readWrap0(final DirectBuffer wrap, final int length) {
        if (length > 0) {
            wrap.wrap(buffer, readOffset, length);
            return true;
        }
        return false;
    }

    @Override
    public ByteBuffer readOrNull() {
        final int readLength = readLength();
        if (readLength > 0) {
            final ByteBuffer byteBuffer = buffer.byteBuffer();
            final int adjustment = buffer.wrapAdjustment();
            byteBuffer.position(readOffset + adjustment);
            byteBuffer.limit(readOffset + adjustment + readLength);
            return byteBuffer;
        }
        return null;
    }

    @Override
    public boolean writeUnsignedInt(final int value) {
        final int writeLength = writeLength();
        if (writeLength >= Integer.BYTES) {
            buffer.putInt(writeOffset, value);
            writeCommit0(Integer.BYTES);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(final DirectBuffer source, final int offset, final int length) {
        final int writeLength = writeLength();
        if (length <= writeLength) {
            buffer.putBytes(writeOffset, source, offset, length);
            writeCommit0(length);
            return true;
        }
        return false;
    }

    @Override
    public boolean write(final ByteBuffer source, final int offset, final int length) {
        final int writeLength = writeLength();
        if (length <= writeLength) {
            buffer.putBytes(writeOffset, source, offset, length);
            writeCommit0(length);
            return true;
        }
        return false;
    }

    @Override
    public boolean writeWrap(final MutableDirectBuffer wrap) {
        return writeWrap0(wrap, writeLength());
    }

    @Override
    public boolean writeWrap(final MutableDirectBuffer wrap, final int maxLength) {
        return writeWrap0(wrap, Math.min(writeLength(), maxLength));
    }

    private boolean writeWrap0(final MutableDirectBuffer wrap, final int length) {
        if (length > 0) {
            wrap.wrap(buffer, writeOffset, length);
            return true;
        }
        return false;
    }

    @Override
    public ByteBuffer writeOrNull() {
        final int writeLength = writeLength();
        if (writeLength > 0) {
            final ByteBuffer byteBuffer = buffer.byteBuffer();
            final int adjustment = buffer.wrapAdjustment();
            byteBuffer.position(writeOffset + adjustment);
            byteBuffer.limit(writeOffset + adjustment + writeLength);
            return byteBuffer;
        }
        return null;
    }

    @Override
    public void readCommit(final int bytes) {
        if (!readSkip(bytes)) {
            throw new IllegalArgumentException("Cannot commit " + bytes + " bytes, only " + writeLength() +
                    " are available for reading");
        }
    }

    @Override
    public boolean readSkip(final int bytes) {
        final int readLength = readLength();
        if (bytes <= readLength) {
            readCommit0(bytes);
            return true;
        }
        return false;
    }

    @Override
    public boolean readSkipEndGap(final int minBytes) {
        final int bytesToEnd = capacity() - readOffset;
        if (bytesToEnd < minBytes) {
            if (readLength() == bytesToEnd) {
                readCommit0(bytesToEnd);
                return true;
            }
        }
        return false;
    }

    private void readCommit0(final int bytes) {
        readOffset += bytes;
        normaliseOffsets();
    }

    @Override
    public void writeCommit(final int bytes) {
        if (!writeSkip(bytes)) {
            throw new IllegalArgumentException("Cannot commit " + bytes + " bytes, only " + writeLength() +
                    " are available for writing");
        }
    }

    @Override
    public boolean writeSkip(final int bytes) {
        final int writeLength = writeLength();
        if (bytes <= writeLength) {
            writeCommit0(bytes);
            return true;
        }
        return false;
    }

    @Override
    public boolean writeSkipEndGap(final int minBytes) {
        final int bytesToEnd = capacity() - writeOffset;
        if (bytesToEnd < minBytes) {
            if (writeLength() == bytesToEnd) {
                writeCommit0(bytesToEnd);
                return true;
            }
        }
        return false;
    }

    private void writeCommit0(final int bytes) {
        writeOffset += bytes;
        normaliseWriteOffset();
    }

    @Override
    public int readLength() {
        return writeOffset > readOffset ? writeOffset - readOffset :
                writeOffset < readOffset || readOffset != 0 ? buffer.capacity() - readOffset : 0;
    }

    @Override
    public int writeLength() {
        return writeOffset >= readOffset ? buffer.capacity() - writeOffset : readOffset - writeOffset;
    }

    @Override
    public int capacity() {
        return buffer.capacity();
    }

    @Override
    public int readOffset() {
        return readOffset;
    }

    @Override
    public int writeOffset() {
        return writeOffset;
    }

    @Override
    public void readOffset(final int offset) {
        final int capacity = capacity();
        this.readOffset = Math.max(0, Math.min(offset, capacity));
        normaliseOffsets();
    }

    @Override
    public void writeOffset(final int offset) {
        final int capacity = capacity();
        this.writeOffset = Math.max(0, Math.min(offset, capacity));
        normaliseWriteOffset();
    }

    @Override
    public void reset(final int readOffset, final int writeOffset) {
        final int capacity = capacity();
        this.readOffset = Math.max(0, Math.min(readOffset, capacity));
        this.writeOffset = Math.max(0, Math.min(writeOffset, capacity));
        normaliseOffsets();
    }

    @Override
    public void reset() {
        this.readOffset = 0;
        this.writeOffset = 0;
    }

    private void normaliseOffsets() {
        if (readOffset == writeOffset) {
            readOffset = 0;
            writeOffset = 0;
            return;
        }
        if (readOffset == capacity()) {
            readOffset = 0;
            return;
        }
        normaliseWriteOffset();
    }

    private void normaliseWriteOffset() {
        if (readOffset > 0 && writeOffset == capacity()) {
            writeOffset = 0;
        }
    }

    @Override
    public String toString() {
        final int capacity = buffer.capacity();
        final int readLength = readLength();
        final StringBuilder sb = new StringBuilder(capacity + 2).append('|');
        for (int i = 0; i < capacity; i++) {
            final byte b = buffer.getByte(i);
            if ((readOffset <= i && i < readOffset + readLength) ||
                    (readOffset + readLength == capacity && i < writeOffset)) {
                sb.append(b >= 32 ? (char)b : '?');
            } else {
                sb.append('.');
            }
        }
        return sb.append('|').toString();
    }
}
