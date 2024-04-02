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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.bytes.Bytes;
import org.agrona.AsciiEncoding;
import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class BytesDirectBuffer implements MutableDirectBuffer {
    private final UnsafeBuffer ub = new UnsafeBuffer(0, 0);
    private Bytes<?> bytes;

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
        throw new UnsupportedOperationException();
    }

    @Override
    public void wrap(DirectBuffer buffer, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void wrap(long address, int length) {
        throw new UnsupportedOperationException();
    }

    public void wrapForWriting(Bytes<?> bytes) {
        final long offset = bytes.writePosition();
        final long address = bytes.addressForWrite(offset);
        final int capacity = (int)Math.min(Integer.MAX_VALUE, bytes.realWriteRemaining());
        if (this.bytes != null) {
            throw new IllegalStateException("already wrapped");
        }
        this.ub.wrap(address, capacity);
        this.bytes = bytes;
    }

    public void unwrap() {
        if (this.bytes != null) {
            ub.wrap(0, 0);
            bytes = null;
        }
    }

    @Override
    public long addressOffset() {
        return ub.addressOffset();
    }

    @Override
    public byte[] byteArray() {
        return null;
    }

    @Override
    public ByteBuffer byteBuffer() {
        return null;
    }

    public Bytes<?> bytes() {
        return bytes;
    }

    @Override
    public void setMemory(int index, int length, byte value) {
        ensureCapacity(index, length);
        ub.setMemory(index, length, value);
    }

    @Override
    public int capacity() {
        return ub.capacity();
    }

    @Override
    public boolean isExpandable() {
        return true;
    }

    @Override
    public void checkLimit(int limit) {
        if (limit < 0) {
            throw new IndexOutOfBoundsException("limit cannot be negative: limit=" + limit);
        } else {
            this.ensureCapacity(limit, 1);
        }
    }

    @Override
    public long getLong(int index, ByteOrder byteOrder) {
        return ub.getLong(index, byteOrder);
    }

    @Override
    public void putLong(int index, long value, ByteOrder byteOrder) {
        ensureCapacity(index, 8);
        ub.putLong(index, value, byteOrder);
    }

    @Override
    public long getLong(int index) {
        return ub.getLong(index);
    }

    @Override
    public void putLong(int index, long value) {
        ensureCapacity(index, 8);
        ub.putLong(index, value);
    }

    @Override
    public int getInt(int index, ByteOrder byteOrder) {
        return ub.getInt(index, byteOrder);
    }

    @Override
    public void putInt(int index, int value, ByteOrder byteOrder) {
        ensureCapacity(index, 4);
        ub.putInt(index, value, byteOrder);
    }

    @Override
    public int getInt(int index) {
        return ub.getInt(index);
    }

    @Override
    public void putInt(int index, int value) {
        ensureCapacity(index, 4);
        ub.putInt(index, value);
    }

    @Override
    public double getDouble(int index, ByteOrder byteOrder) {
        return ub.getDouble(index, byteOrder);
    }

    @Override
    public void putDouble(int index, double value, ByteOrder byteOrder) {
        ensureCapacity(index, 8);
        ub.putDouble(index, value, byteOrder);
    }

    @Override
    public double getDouble(int index) {
        return ub.getDouble(index);
    }

    @Override
    public void putDouble(int index, double value) {
        ensureCapacity(index, 8);
        ub.putDouble(index, value);
    }

    @Override
    public float getFloat(int index, ByteOrder byteOrder) {
        return ub.getFloat(index, byteOrder);
    }

    @Override
    public void putFloat(int index, float value, ByteOrder byteOrder) {
        ensureCapacity(index, 4);
        ub.putFloat(index, value, byteOrder);
    }

    @Override
    public float getFloat(int index) {
        return ub.getFloat(index);
    }

    @Override
    public void putFloat(int index, float value) {
        ensureCapacity(index, 4);
        ub.putFloat(index, value);
    }

    @Override
    public short getShort(int index, ByteOrder byteOrder) {
        return ub.getShort(index, byteOrder);
    }

    @Override
    public void putShort(int index, short value, ByteOrder byteOrder) {
        ensureCapacity(index, 2);
        ub.putShort(index, value, byteOrder);
    }

    @Override
    public short getShort(int index) {
        return ub.getShort(index);
    }

    @Override
    public void putShort(int index, short value) {
        ensureCapacity(index, 2);
        ub.putShort(index, value);
    }

    @Override
    public byte getByte(int index) {
        return ub.getByte(index);
    }

    @Override
    public void putByte(int index, byte value) {
        ensureCapacity(index, 1);
        ub.putByte(index, value);
    }

    @Override
    public void getBytes(int index, byte[] dst) {
        ub.getBytes(index, dst);
    }

    @Override
    public void getBytes(int index, byte[] dst, int offset, int length) {
        ub.getBytes(index, dst, offset, length);
    }

    @Override
    public void getBytes(int index, MutableDirectBuffer dstBuffer, int dstIndex, int length) {
        ub.getBytes(index, dstBuffer, dstIndex, length);
    }

    @Override
    public void getBytes(int index, ByteBuffer dstBuffer, int length) {
        ub.getBytes(index, dstBuffer, length);
    }

    @Override
    public void getBytes(int index, ByteBuffer dstBuffer, int dstOffset, int length) {
        ub.getBytes(index, dstBuffer, dstOffset, length);
    }

    @Override
    public void putBytes(int index, byte[] src) {
        putBytes(index, src, 0, src.length);
    }

    @Override
    public void putBytes(int index, byte[] src, int offset, int length) {
        ensureCapacity(index, length);
        ub.putBytes(index, src, offset, length);
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
        ub.putBytes(index, srcBuffer, srcIndex, length);
    }

    @Override
    public void putBytes(int index, DirectBuffer srcBuffer, int srcIndex, int length) {
        ensureCapacity(index, length);
        ub.putBytes(index, srcBuffer, srcIndex, length);
    }

    @Override
    public char getChar(int index, ByteOrder byteOrder) {
        return ub.getChar(index, byteOrder);
    }

    @Override
    public void putChar(int index, char value, ByteOrder byteOrder) {
        ensureCapacity(index, 2);
        ub.putChar(index, value, byteOrder);
    }

    @Override
    public char getChar(int index) {
        return ub.getChar(index);
    }

    @Override
    public void putChar(int index, char value) {
        ensureCapacity(index, 2);
        ub.putChar(index, value);
    }

    @Override
    public String getStringAscii(int index) {
        return ub.getStringAscii(index);
    }

    @Override
    public int getStringAscii(int index, Appendable appendable) {
        return ub.getStringAscii(index, appendable);
    }

    @Override
    public String getStringAscii(int index, ByteOrder byteOrder) {
        return ub.getStringAscii(index, byteOrder);
    }

    @Override
    public int getStringAscii(int index, Appendable appendable, ByteOrder byteOrder) {
        return ub.getStringAscii(index, appendable, byteOrder);
    }

    @Override
    public String getStringAscii(int index, int length) {
        return ub.getStringAscii(index, length);
    }

    @Override
    public int getStringAscii(int index, int length, Appendable appendable) {
        return ub.getStringAscii(index, length, appendable);
    }

    @Override
    public int putStringAscii(int index, String value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return ub.putStringAscii(index, value);
    }

    @Override
    public int putStringAscii(int index, CharSequence value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return ub.putStringAscii(index, value);
    }

    @Override
    public int putStringAscii(int index, String value, ByteOrder byteOrder) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return ub.putStringAscii(index, value, byteOrder);
    }

    @Override
    public int putStringAscii(int index, CharSequence value, ByteOrder byteOrder) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length + 4);
        return ub.putStringAscii(index, value, byteOrder);
    }

    @Override
    public String getStringWithoutLengthAscii(int index, int length) {
        return ub.getStringWithoutLengthAscii(index, length);
    }

    @Override
    public int getStringWithoutLengthAscii(int index, int length, Appendable appendable) {
        return ub.getStringWithoutLengthAscii(index, length, appendable);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, String value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length);
        return ub.putStringWithoutLengthAscii(index, value);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, CharSequence value) {
        final int length = value != null ? value.length() : 0;
        ensureCapacity(index, length);
        return ub.putStringWithoutLengthAscii(index, value);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, String value, int valueOffset, int length) {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;
        ensureCapacity(index, len);
        return ub.putStringWithoutLengthAscii(index, value, valueOffset, length);
    }

    @Override
    public int putStringWithoutLengthAscii(int index, CharSequence value, int valueOffset, final int length) {
        final int len = value != null ? Math.min(value.length() - valueOffset, length) : 0;
        ensureCapacity(index, len);
        return ub.putStringWithoutLengthAscii(index, value, valueOffset, length);
    }

    @Override
    public String getStringUtf8(int index) {
        return ub.getStringUtf8(index);
    }

    @Override
    public String getStringUtf8(int index, ByteOrder byteOrder) {
        return ub.getStringUtf8(index, byteOrder);
    }

    @Override
    public String getStringUtf8(int index, int length) {
        return ub.getStringUtf8(index, length);
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
            ub.putInt(index, bytes.length);
            ub.putBytes(index + 4, bytes);
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
            ub.putInt(index, bits);
            ub.putBytes(index + 4, bytes);
            return 4 + bytes.length;
        }
    }

    @Override
    public String getStringWithoutLengthUtf8(int index, int length) {
        return ub.getStringWithoutLengthUtf8(index, length);
    }

    @Override
    public int putStringWithoutLengthUtf8(int index, String value) {
        final byte[] bytes = value != null ? value.getBytes(StandardCharsets.UTF_8) : BufferUtil.NULL_BYTES;
        ensureCapacity(index, bytes.length);
        ub.putBytes(index, bytes);
        return bytes.length;
    }

    @Override
    public int parseNaturalIntAscii(int index, int length) {
        return ub.parseNaturalIntAscii(index, length);
    }

    @Override
    public long parseNaturalLongAscii(int index, int length) {
        return ub.parseNaturalLongAscii(index, length);
    }

    @Override
    public int parseIntAscii(int index, int length) {
        return ub.parseIntAscii(index, length);
    }

    @Override
    public long parseLongAscii(int index, int length) {
        return ub.parseLongAscii(index, length);
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
                ub.putByte(index, (byte)'-');
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
                ub.putByte(i + start, (byte)('0' + remainder));
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
                ub.putByte(i + index, (byte)('0' + remainder));
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
                ub.putByte(i + index, (byte)((int)('0' + remainder)));
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
                ub.putByte(i + start, (byte)((int)('0' + remainder)));
                --i;
            }

            return length;
        }
    }

    public void ensureCapacity(int index, int length) {
        if (bytes == null) {
            throw new IndexOutOfBoundsException("bytes is null hence capacity is 0");
        }
        if (index >= 0 && length >= 0) {
            final long requiredCapacity = (long)index + (long)length;
            if (requiredCapacity > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("required capacity exceeds maximum: " + requiredCapacity + " > " +
                        Integer.MAX_VALUE);
            }
            ensureCapacity0((int)requiredCapacity);
        } else {
            throw new IndexOutOfBoundsException("negative value: index=" + index + " length=" + length);
        }
    }

    public void ensureCapacity(int requiredCapacity) {
        if (bytes == null) {
            throw new IndexOutOfBoundsException("bytes is null hence capacity is 0");
        }
        if (requiredCapacity >= 0) {
            ensureCapacity0(requiredCapacity);
        } else {
            throw new IndexOutOfBoundsException("negative capacity: requiredCapacity=" + requiredCapacity);
        }
    }

    private void ensureCapacity0(int requiredCapacity) {
        bytes.ensureCapacity(requiredCapacity);
    }

    @Override
    public void boundsCheck(int index, int length) {
        ub.boundsCheck(index, length);
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
            BytesDirectBuffer that = (BytesDirectBuffer)obj;
            return this.compareTo(that) == 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ub.hashCode();
    }

    @Override
    public int compareTo(DirectBuffer that) {
        return ub.compareTo(that);
    }

    @Override
    public String toString() {
        return "BytesDirectBuffer{address=" + addressOffset() + ", capacity=" + capacity() + ", bytes=" + this.bytes + '}';
    }
}