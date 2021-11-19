/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.log;

import org.agrona.AsciiEncoding;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * A {@link DirectBuffer} that wraps another buffer and preserves the underlying buffer's extensibility.
 */
public class ExpandableDirectBuffer implements MutableDirectBuffer {

    private MutableDirectBuffer buffer;
    private int offset;

    /**
     * Create an unwrapped {@link ExpandableDirectBuffer}.
     */
    public ExpandableDirectBuffer() {
        super();
    }

    /**
     * @param buffer the buffer to wrap
     * Create an {@link ExpandableDirectBuffer} wrapping the given buffer.
     */
    public ExpandableDirectBuffer(final MutableDirectBuffer buffer) {
        this.buffer = buffer;
        this.offset = 0;
    }

    /**
     * @param buffer the buffer to wrap
     * @param offset the offset at which this view begins
     * Create an {@link ExpandableDirectBuffer} wrapping a section of the given buffer.
     */
    public ExpandableDirectBuffer(final MutableDirectBuffer buffer, final int offset) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Negative offset: " + offset);
        }
        this.buffer = buffer;
        this.offset = offset;
    }

    @Override
    public void wrap(byte[] buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void wrap(byte[] buffer, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void wrap(ByteBuffer buffer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void wrap(ByteBuffer buffer, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void wrap(DirectBuffer buffer) {
        if (this.buffer != buffer) {
            if (buffer != null && !(buffer instanceof MutableDirectBuffer)) {
                throw new IllegalArgumentException("Buffer must be a mutable buffer: " + buffer);
            }
            this.buffer = (MutableDirectBuffer)buffer;
        }
        this.offset = 0;
    }

    public void wrap(MutableDirectBuffer buffer, int offset) {
        if (offset < 0) {
            throw new IndexOutOfBoundsException("Negative offset: " + offset);
        }
        if (this.buffer != buffer) {
            this.buffer = buffer;
        }
        this.offset = offset;
    }

    @Override
    public void wrap(DirectBuffer buffer, int offset, int length) {
        if (buffer != null && !(buffer instanceof MutableDirectBuffer)) {
            throw new IllegalArgumentException("Buffer must be a mutable buffer: " + buffer);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException("Negative length: " + length);
        }
        wrap((MutableDirectBuffer)buffer, length);
    }

    @Override
    public void wrap(long address, int length) {
        throw new UnsupportedOperationException();
    }

    public void unwrap() {
        this.buffer = null;
        this.offset = 0;
    }

    public MutableDirectBuffer buffer() {
        return buffer;
    }

    public int offset() {
        return offset;
    }

    @Override
    public long addressOffset() {
        return buffer == null ? 0 : buffer.addressOffset() + offset;
    }

    @Override
    public byte[] byteArray() {
        return buffer == null ? null : buffer.byteArray();
    }

    @Override
    public ByteBuffer byteBuffer() {
        return buffer == null ? null : buffer.byteBuffer();
    }

    @Override
    public void setMemory(int index, int length, byte value) {
        ensureCapacity(index, length);
        buffer.setMemory(index + offset, length, value);
    }

    @Override
    public int capacity() {
        return buffer.capacity() - offset;
    }

    @Override
    public boolean isExpandable() {
        return buffer.isExpandable();
    }

    @Override
    public void checkLimit(int limit) {
        if (limit < 0) {
            throw new IndexOutOfBoundsException("limit cannot be negative: limit=" + limit);
        } else {
            this.ensureCapacity(limit + offset, 1);
        }
    }

    @Override
    public long getLong(int index, ByteOrder byteOrder) {
        return buffer.getLong(index + offset, byteOrder);
    }

    @Override
    public void putLong(int index, long value, ByteOrder byteOrder) {
        ensureCapacity(index, 8);
        buffer.putLong(index + offset, value, byteOrder);
    }

    @Override
    public long getLong(int index) {
        return buffer.getLong(index + offset);
    }

    @Override
    public void putLong(int index, long value) {
        ensureCapacity(index, 8);
        buffer.putLong(index + offset, value);
    }

    @Override
    public int getInt(int index, ByteOrder byteOrder) {
        return buffer.getInt(index + offset, byteOrder);
    }

    @Override
    public void putInt(int index, int value, ByteOrder byteOrder) {
        ensureCapacity(index, 4);
        buffer.putInt(index + offset, value, byteOrder);
    }

    @Override
    public int getInt(int index) {
        return buffer.getInt(index + offset);
    }

    @Override
    public void putInt(int index, int value) {
        ensureCapacity(index, 4);
        buffer.putInt(index + offset, value);
    }

    @Override
    public double getDouble(int index, ByteOrder byteOrder) {
        return buffer.getDouble(index + offset, byteOrder);
    }

    @Override
    public void putDouble(int index, double value, ByteOrder byteOrder) {
        ensureCapacity(index, 8);
        buffer.putDouble(index + offset, value, byteOrder);
    }

    @Override
    public double getDouble(int index) {
        return buffer.getDouble(index + offset);
    }

    @Override
    public void putDouble(int index, double value) {
        ensureCapacity(index, 8);
        buffer.putDouble(index + offset, value);
    }

    @Override
    public float getFloat(int index, ByteOrder byteOrder) {
        return buffer.getFloat(index + offset, byteOrder);
    }

    @Override
    public void putFloat(int index, float value, ByteOrder byteOrder) {
        ensureCapacity(index, 4);
        buffer.putFloat(index + offset, value, byteOrder);
    }

    @Override
    public float getFloat(int index) {
        return buffer.getFloat(index + offset);
    }

    @Override
    public void putFloat(int index, float value) {
        ensureCapacity(index, 4);
        buffer.putFloat(index + offset, value);
    }

    @Override
    public short getShort(int index, ByteOrder byteOrder) {
        return buffer.getShort(index + offset, byteOrder);
    }

    @Override
    public void putShort(int index, short value, ByteOrder byteOrder) {
        ensureCapacity(index, 2);
        buffer.putShort(index + offset, value, byteOrder);
    }

    @Override
    public short getShort(int index) {
        return buffer.getShort(index + offset);
    }

    @Override
    public void putShort(int index, short value) {
        ensureCapacity(index, 2);
        buffer.putShort(index + offset, value);
    }

    @Override
    public byte getByte(int index) {
        return buffer.getByte(index + offset);
    }

    @Override
    public void putByte(int index, byte value) {
        ensureCapacity(index, 1);
        buffer.putByte(index + offset, value);
    }

    @Override
    public void getBytes(int index, byte[] dst) {
        buffer.getBytes(index + offset, dst);
    }

    @Override
    public void getBytes(int index, byte[] dst, int offset, int length) {
        buffer.getBytes(index + offset, dst, offset, length);
    }

    @Override
    public void getBytes(int index, MutableDirectBuffer dstBuffer, int dstIndex, int length) {
        buffer.getBytes(index + offset, dstBuffer, dstIndex, length);
    }

    @Override
    public void getBytes(int index, ByteBuffer dstBuffer, int length) {
        buffer.getBytes(index + offset, dstBuffer, length);
    }

    @Override
    public void getBytes(int index, ByteBuffer dstBuffer, int dstOffset, int length) {
        buffer.getBytes(index + offset, dstBuffer, dstOffset, length);
    }

    @Override
    public void putBytes(int index, byte[] src) {
        putBytes(index, src, 0, src.length);
    }

    @Override
    public void putBytes(int index, byte[] src, int offset, int length) {
        ensureCapacity(index, length);
        buffer.putBytes(index + offset, src, offset, length);
    }

    @Override
    public void putBytes(int index, ByteBuffer srcBuffer, int length) {
        final int srcIndex = srcBuffer.position();
        putBytes(index, srcBuffer, srcIndex, length);
        srcBuffer.position(srcIndex + length);
    }

    @Override
    public void putBytes(int index, ByteBuffer srcBuffer, int srcIndex, int length) {
        ensureCapacity(index, length);
        buffer.putBytes(index + offset, srcBuffer, srcIndex, length);
    }

    @Override
    public void putBytes(int index, DirectBuffer srcBuffer, int srcIndex, int length) {
        ensureCapacity(index, length);
        buffer.putBytes(index + offset, srcBuffer, srcIndex, length);
    }

    @Override
    public char getChar(int index, ByteOrder byteOrder) {
        return buffer.getChar(index + offset, byteOrder);
    }

    @Override
    public void putChar(int index, char value, ByteOrder byteOrder) {
        ensureCapacity(index, 2);
        buffer.putChar(index + offset, value, byteOrder);
    }

    @Override
    public char getChar(int index) {
        return buffer.getChar(index + offset);
    }

    @Override
    public void putChar(int index, char value) {
        ensureCapacity(index, 2);
        buffer.putChar(index + offset, value);
    }

    @Override
    public String getStringAscii(int index) {
        return buffer.getStringAscii(index + offset);
    }

    @Override
    public int getStringAscii(int index, Appendable appendable) {
        return buffer.getStringAscii(index + offset, appendable);
    }

    @Override
    public String getStringAscii(int index, ByteOrder byteOrder) {
        return buffer.getStringAscii(index + offset, byteOrder);
    }

    @Override
    public int getStringAscii(int index, Appendable appendable, ByteOrder byteOrder) {
        return buffer.getStringAscii(index + offset, appendable, byteOrder);
    }

    @Override
    public String getStringAscii(int index, int length) {
        return buffer.getStringAscii(index + offset, length);
    }

    @Override
    public int getStringAscii(int index, int length, Appendable appendable) {
        return buffer.getStringAscii(index + offset, length, appendable);
    }

    @Override
    public int putStringAscii(int index, String value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return buffer.putStringAscii(index + offset, value);
    }

    @Override
    public int putStringAscii(int index, CharSequence value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return buffer.putStringAscii(index + offset, value);
    }

    @Override
    public int putStringAscii(int index, String value, ByteOrder byteOrder) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return buffer.putStringAscii(index + offset, value, byteOrder);
    }

    @Override
    public int putStringAscii(int index, CharSequence value, ByteOrder byteOrder) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return buffer.putStringAscii(index + offset, value, byteOrder);
    }

    @Override
    public String getStringWithoutLengthAscii(int index, int length) {
        return buffer.getStringWithoutLengthAscii(index + offset, length);
    }

    @Override
    public int getStringWithoutLengthAscii(int index, int length, Appendable appendable) {
        return buffer.getStringWithoutLengthAscii(index + offset, length, appendable);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, String value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length);
        return buffer.putStringWithoutLengthAscii(index + offset, value);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, CharSequence value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length);
        return buffer.putStringWithoutLengthAscii(index + offset, value);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, String value, int valueOffset, int length) {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;
        ensureCapacity(index, len);
        return buffer.putStringWithoutLengthAscii(index + offset, value, valueOffset, length);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, CharSequence value, int valueOffset, int length) {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;
        ensureCapacity(index, len);
        return buffer.putStringWithoutLengthAscii(index + offset, value, valueOffset, length);
    }

    @Override
    public String getStringUtf8(int index) {
        return buffer.getStringUtf8(index + offset);
    }

    @Override
    public String getStringUtf8(int index, ByteOrder byteOrder) {
        return buffer.getStringUtf8(index + offset, byteOrder);
    }

    @Override
    public String getStringUtf8(int index, int length) {
        return buffer.getStringUtf8(index + offset, length);
    }

    @Override
    public int putStringUtf8(int index, String value) {
        return putStringUtf8(index, value, 2147483647);
    }

    @Override
    public int putStringUtf8(int index, String value, ByteOrder byteOrder) {
        return putStringUtf8(index, value, byteOrder, 2147483647);
    }

    @Override
    public int putStringUtf8(int index, String value, int maxEncodedLength) {
        final byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : BufferUtil.NULL_BYTES;
        if (bytes.length > maxEncodedLength) {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedLength);
        } else {
            ensureCapacity(index, 4 + bytes.length);
            buffer.putInt(index + offset, bytes.length);
            buffer.putBytes(index + offset + 4, bytes);
            return 4 + bytes.length;
        }
    }

    @Override
    public int putStringUtf8(int index, String value, ByteOrder byteOrder, int maxEncodedLength) {
        final byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : BufferUtil.NULL_BYTES;
        if (bytes.length > maxEncodedLength) {
            throw new IllegalArgumentException("Encoded string larger than maximum size: " + maxEncodedLength);
        } else {
            ensureCapacity(index, 4 + bytes.length);
            int bits = bytes.length;
            if (BufferUtil.NATIVE_BYTE_ORDER != byteOrder) {
                bits = Integer.reverseBytes(bits);
            }
            buffer.putInt(index + offset, bits);
            buffer.putBytes(index + offset + 4, bytes);
            return 4 + bytes.length;
        }
    }

    @Override
    public String getStringWithoutLengthUtf8(int index, int length) {
        return buffer.getStringWithoutLengthUtf8(index + offset, length);
    }

    @Override
    public int putStringWithoutLengthUtf8(int index, String value) {
        final byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : BufferUtil.NULL_BYTES;
        ensureCapacity(index, bytes.length);
        buffer.putBytes(index + offset, bytes);
        return bytes.length;
    }

    @Override
    public int parseNaturalIntAscii(int index, int length) {
        return buffer.parseNaturalIntAscii(index + offset, length);
    }

    @Override
    public long parseNaturalLongAscii(int index, int length) {
        return buffer.parseNaturalLongAscii(index + offset, length);
    }

    @Override
    public int parseIntAscii(int index, int length) {
        return buffer.parseIntAscii(index + offset, length);
    }

    @Override
    public long parseLongAscii(int index, int length) {
        return buffer.parseLongAscii(index + offset, length);
    }

    @Override
    public int putIntAscii(int index, int value) {
        if (value == 0) {
            putByte(index, (byte)'0');
            return 1;
        } else if (value == -2147483648) {
            putBytes(index, AsciiEncoding.MIN_INTEGER_VALUE);
            return AsciiEncoding.MIN_INTEGER_VALUE.length;
        } else {
            int start = index;
            int quotient = value;
            int length = 1;
            if (value < 0) {
                putByte(index, (byte)'-');
                start = index + 1;
                ++length;
                quotient = -value;
            }

            int i = AsciiEncoding.digitCount(quotient) - 1;
            length += i;
            ensureCapacity(index, length);

            while(i >= 0) {
                int remainder = quotient % 10;
                quotient /= 10;
                buffer.putByte(i + offset + start, (byte)('0' + remainder));
                --i;
            }

            return length;
        }
    }

    @Override
    public int putNaturalIntAscii(int index, int value) {
        if (value == 0) {
            putByte(index, (byte)'0');
            return 1;
        } else {
            int i = AsciiEncoding.digitCount(value) - 1;
            int length = i + 1;
            ensureCapacity(index, length);

            for(int quotient = value; i >= 0; --i) {
                int remainder = quotient % 10;
                quotient /= 10;
                buffer.putByte(i + offset + index, (byte)('0' + remainder));
            }

            return length;
        }
    }

    @Override
    public void putNaturalPaddedIntAscii(int offset, int length, int value) {
        final int end = offset + length;
        int remainder = value;

        for(int index = end - 1; index >= offset; --index) {
            int digit = remainder % 10;
            remainder /= 10;
            putByte(index, (byte)('0' + digit));
        }

        if (remainder != 0) {
            throw new NumberFormatException(String.format("Cannot write %d in %d bytes", value, length));
        }
    }

    @Override
    public int putNaturalIntAsciiFromEnd(int value, int endExclusive) {
        int remainder = value;
        int index = endExclusive;

        while(remainder > 0) {
            --index;
            int digit = remainder % 10;
            remainder /= 10;
            putByte(index, (byte)('0' + digit));
        }

        return index;
    }

    @Override
    public int putNaturalLongAscii(int index, long value) {
        if (value == 0L) {
            putByte(index, (byte)'0');
            return 1;
        } else {
            int i = AsciiEncoding.digitCount(value) - 1;
            int length = i + 1;
            ensureCapacity(index, length);

            for(long quotient = value; i >= 0; --i) {
                long remainder = quotient % 10L;
                quotient /= 10L;
                buffer.putByte(i + offset + index, (byte)((int)('0' + remainder)));
            }

            return length;
        }
    }

    @Override
    public int putLongAscii(int index, long value) {
        if (value == 0L) {
            putByte(index, (byte)48);
            return 1;
        } else if (value == -9223372036854775808L) {
            putBytes(index, AsciiEncoding.MIN_LONG_VALUE);
            return AsciiEncoding.MIN_LONG_VALUE.length;
        } else {
            int start = index;
            long quotient = value;
            int length = 1;
            if (value < 0L) {
                putByte(index, (byte)'-');
                start = index + 1;
                ++length;
                quotient = -value;
            }

            int i = AsciiEncoding.digitCount(quotient) - 1;
            length += i;
            ensureCapacity(index, length);

            while(i >= 0) {
                long remainder = quotient % 10L;
                quotient /= 10L;
                buffer.putByte(i + offset + start, (byte)((int)('0' + remainder)));
                --i;
            }

            return length;
        }
    }

    public void ensureCapacity(int index, int length) {
        if (buffer == null) {
            throw new IndexOutOfBoundsException("no buffer wrapped hence capacity is 0");
        }
        if (index >= 0 && length >= 0) {
            final long requiredCapacity = (long)index + (long)offset + (long)length;
            if (requiredCapacity <= buffer.capacity()) {
                return;
            }
            if (requiredCapacity > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("required capacity exceeds maximum: " + requiredCapacity + " > " +
                        Integer.MAX_VALUE);
            }
            buffer.checkLimit((int)(requiredCapacity - 1));
        } else {
            throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
        }
    }

    @Override
    public void boundsCheck(int index, int length) {
        buffer.boundsCheck(index, length);
    }

    @Override
    public int wrapAdjustment() {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            ExpandableDirectBuffer that = (ExpandableDirectBuffer)obj;
            return this.compareTo(that) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return buffer.hashCode();
    }

    @Override
    public int compareTo(DirectBuffer that) {
        return buffer.compareTo(that);
    }

    @Override
    public String toString() {
        return "ExpandableDirectBuffer{buffer=" + buffer + ", offset=" + offset + '}';
    }
}
