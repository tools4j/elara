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
package org.tools4j.elara.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.FrameDescriptor.FRAME_SIZE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.RESERVED_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.VERSION_OFFSET;

/**
 * A flyweight header for reading and writing general header data laid out as per {@link FrameDescriptor} definition.
 */
public final class FlyweightHeader implements Flyweight<FlyweightHeader>, Header {

    private final int headerLength;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    public FlyweightHeader(final int headerLength) {
        if (headerLength < HEADER_LENGTH) {
            throw new IllegalArgumentException("Invalid header length + " + headerLength);
        }
        this.headerLength = headerLength;
    }

    public int headerLength() {
        return headerLength;
    }

    public MutableDirectBuffer buffer() {
        return buffer;
    }

    @Override
    public FlyweightHeader wrap(final DirectBuffer buffer, final int offset) {
        Version.validate(buffer.getByte(offset + VERSION_OFFSET));
        return wrapSilently(buffer, offset);
    }

    public FlyweightHeader wrapSilently(final DirectBuffer buffer, final int offset) {
        this.buffer.wrap(buffer, offset, headerLength);
        return this;
    }

    @Override
    public boolean valid() {
        return buffer.capacity() >= headerLength;
    }

    @Override
    public FlyweightHeader reset() {
        buffer.wrap(0, 0);
        return this;
    }
    @Override
    public int version() {
        return version(buffer);
    }

    public static int version(final DirectBuffer buffer) {
        return 0xff & buffer.getByte(VERSION_OFFSET);
    }

    @Override
    public byte type() {
        return type(buffer);
    }

    public static byte type(final DirectBuffer buffer) {
        return buffer.getByte(TYPE_OFFSET);
    }

    public static void writeType(final byte type, final MutableDirectBuffer dst) {
        dst.putByte(TYPE_OFFSET, type);
    }

    @Override
    public short reserved() {
        return reserved(buffer);
    }

    public static short reserved(final DirectBuffer buffer) {
        return buffer.getShort(RESERVED_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int frameSize() {
        return frameSize(buffer);
    }

    public static int frameSize(final DirectBuffer buffer) {
        return buffer.getInt(FRAME_SIZE_OFFSET, LITTLE_ENDIAN);
    }

    public static void writeFrameSize(final int frameSize, final MutableDirectBuffer dst) {
        assert frameSize >= 0;
        dst.putInt(FRAME_SIZE_OFFSET, frameSize, LITTLE_ENDIAN);
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        dst.putBytes(dstOffset + HEADER_OFFSET, buffer, 0, HEADER_LENGTH);
        return HEADER_LENGTH;
    }

    public static int write(final byte type,
                            final short reserved,
                            final int frameSize,
                            final MutableDirectBuffer dst, final int dstOffset) {
        assert frameSize >= 0;
        dst.putLong(dstOffset + HEADER_OFFSET,
                (0xffL & Version.CURRENT) |
                        ((0xffL & type) << 8) |
                        ((0xffffL & reserved) << 16) |
                        ((0x7fffffffL & frameSize) << 32),
                LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static int write(final byte type,
                            final short reserved,
                            final int frameSize,
                            final boolean flag,
                            final MutableDirectBuffer dst, final int dstOffset) {
        assert frameSize >= 0;
        dst.putLong(dstOffset + HEADER_OFFSET,
                (0xffL & Version.CURRENT) |
                        ((0xffL & type) << 8) |
                        ((0xffffL & reserved) << 16) |
                        (((flag ? (0x80000000L | (0x7fffffffL & frameSize)) : (0x7fffffffL & frameSize))) << 32),
                LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightHeader{");
        if (valid()) {
            dst.append("version=").append(version());
            dst.append("|type=").append(type());
            dst.append("|reserved=").append(reserved());
            dst.append("|frame-size=").append(frameSize());
        } else {
            dst.append("???");
        }
        dst.append('}');
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(128)).toString();
    }
}
