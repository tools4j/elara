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
package org.tools4j.elara.samples.bank.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.jetbrains.annotations.NotNull;
import org.tools4j.elara.logging.Printable;
import org.tools4j.elara.store.ExpandableDirectBuffer;

/**
 * Simple ASCII string backed by a {@link DirectBuffer} for GC friendly handling of strings.  The implementation is very
 * similar to {@link org.agrona.AsciiSequenceView} but with equals and hash code implementation added to store accounts
 * in hash maps, as well as some additional convenience methods.
 */
public class AsciiString implements CharSequence, Printable {

    private DirectBuffer buffer;
    private int offset;
    private int length;

    public AsciiString() {
        super();
    }

    public AsciiString(final CharSequence value) {
        set(value);
    }

    public AsciiString reset() {
        buffer = null;
        offset = 0;
        length = 0;
        return this;
    }

    private boolean createBuffer(final int length) {
        if (buffer == null || !(buffer instanceof MutableDirectBuffer)) {
            return true;
        }
        return length > buffer.capacity() &&
                !(buffer instanceof ExpandableArrayBuffer) &&
                !(buffer instanceof ExpandableDirectBuffer) &&
                !(buffer instanceof ExpandableDirectByteBuffer);
    }

    public AsciiString set(final CharSequence value) {
        final int valueLength = value.length();
        if (createBuffer(valueLength)) {
            buffer = new ExpandableArrayBuffer(Math.max(64, valueLength));
            offset = 0;
        }
        ((MutableDirectBuffer)buffer).putStringWithoutLengthAscii(offset, value);
        length = valueLength;
        return this;
    }

    public AsciiString wrap(final DirectBuffer buffer, final int offset, final int length) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset=" + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("length=" + length);
        }
        if (offset + length > buffer.capacity()) {
            throw new IllegalArgumentException("offset=" + offset + " length=" + length + " buffer-capacity=" + buffer.capacity());
        }
        this.buffer = buffer;
        this.offset = offset;
        this.length = length;
        return this;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(final int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Illegal index " + index + " for length " + length);
        }
        return charAt0(index);
    }

    private char charAt0(final int index) {
        return (char)buffer.getByte(offset + index);
    }

    @NotNull
    @Override
    public CharSequence subSequence(final int start, final int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException("start=" + start);
        }
        if (end > length) {
            throw new StringIndexOutOfBoundsException("end=" + end + " length=" + length);
        }
        if (end - start < 0) {
            throw new StringIndexOutOfBoundsException("start=" + start + " end=" + end);
        }
        return new AsciiString().wrap(buffer, offset + start, end - start);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < length; i++) {
            hash <<= 8;
            hash ^= charAt0(i);
        }
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final AsciiString other = (AsciiString)obj;
        if (other.length != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (other.charAt0(i) != charAt0(i)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        if (buffer != null && length > 0) {
            buffer.getStringWithoutLengthAscii(offset, length, dst);
        }
        return dst;
    }

    @Override
    public String toString() {
        if (buffer != null && length > 0) {
            return buffer.getStringWithoutLengthAscii(offset, length);
        }
        return "";
    }
}
