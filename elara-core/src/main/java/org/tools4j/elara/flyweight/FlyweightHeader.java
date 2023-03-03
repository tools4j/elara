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

import static org.tools4j.elara.flyweight.FrameDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.SOURCE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.SOURCE_SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.TIME_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.VERSION_OFFSET;

public class FlyweightHeader implements Flyweight<FlyweightHeader>, Header, Frame {

    private final MutableDirectBuffer header = new UnsafeBuffer(0, 0);

    public FlyweightHeader init(final int sourceId,
                                final int type,
                                final long sourceSeq,
                                final long time,
                                final byte flags,
                                final short index,
                                final int payloadLSize,
                                final MutableDirectBuffer dst,
                                final int dstOffset) {
        writeTo(sourceId, type, sourceSeq, time, flags, index, payloadLSize, dst, dstOffset);
        return initSilent(dst, dstOffset);
    }

    public FlyweightHeader init(final Header header, final MutableDirectBuffer dst, final int dstOffset) {
        return init(header.sourceId(), header.type(), header.sourceSequence(), header.time(), header.flags(), header.index(),
                header.payloadSize(), dst, dstOffset);
    }

    @Override
    public FlyweightHeader init(final DirectBuffer src, final int srcOffset) {
        final int length = src.capacity() - srcOffset;
        if (length < HEADER_LENGTH) {
            throw new IllegalArgumentException("Invalid frame header, expected min length + " + HEADER_LENGTH + " but found only " + length);
        }
        Version.validate(src.getByte(srcOffset + VERSION_OFFSET));
        return initSilent(src, srcOffset);
    }

    public FlyweightHeader initSilent(final DirectBuffer src, final int srcOffset) {
        this.header.wrap(src, srcOffset, HEADER_LENGTH);
        return this;
    }

    public boolean valid() {
        return header.capacity() >= HEADER_LENGTH;
    }

    public FlyweightHeader reset() {
        header.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return this;
    }

    @Override
    public int sourceId() {
        return header.getInt(SOURCE_OFFSET);
    }

    @Override
    public int type() {
        return header.getInt(TYPE_OFFSET);
    }

    @Override
    public long sourceSequence() {
        return header.getLong(SOURCE_SEQUENCE_OFFSET);
    }

    @Override
    public long time() {
        return header.getLong(TIME_OFFSET);
    }

    @Override
    public short version() {
        return (short)(0xff & header.getByte(VERSION_OFFSET));
    }

    @Override
    public byte flags() {
        return header.getByte(FLAGS_OFFSET);
    }

    @Override
    public short index() {
        return header.getShort(INDEX_OFFSET);
    }

    @Override
    public int payloadSize() {
        return header.getInt(PAYLOAD_SIZE_OFFSET);
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        dst.putBytes(dstOffset + HEADER_OFFSET, header, 0, HEADER_LENGTH);
        return HEADER_LENGTH;
    }

    public static int writeTo(final int sourceId,
                              final int type,
                              final long sourceSeq,
                              final long time,
                              final byte flags,
                              final short index,
                              final int payloadLSize,
                              final MutableDirectBuffer dst,
                              final int dstOffset) {
        dst.putInt(dstOffset + SOURCE_OFFSET, sourceId);
        dst.putInt(dstOffset + TYPE_OFFSET, type);
        dst.putLong(dstOffset + SOURCE_SEQUENCE_OFFSET, sourceSeq);
        dst.putLong(dstOffset + TIME_OFFSET, time);
        dst.putByte(dstOffset + VERSION_OFFSET, Version.CURRENT);
        dst.putByte(dstOffset + FLAGS_OFFSET, flags);
        dst.putShort(dstOffset + INDEX_OFFSET, index);
        dst.putInt(dstOffset + PAYLOAD_SIZE_OFFSET, payloadLSize);
        return HEADER_LENGTH;
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightHeader{");
        if (valid()) {
            dst.append("version=").append(version());
            dst.append("|source-id=").append(sourceId());
            dst.append("|source-seq=").append(sourceSequence());
            dst.append("|index=").append(index());
            dst.append("|flags=").append(Flags.toString(header().flags()));
            dst.append("|type=").append(type());
            dst.append("|time=").append(time());
            dst.append("|payload-size=").append(payloadSize());
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
