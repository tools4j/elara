/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.log.Flyweight;

import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.INPUT_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.TIME_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.VERSION_OFFSET;

public class FlyweightHeader implements Flyweight<FlyweightHeader>, Header, Frame {

    private final MutableDirectBuffer header = new UnsafeBuffer(0, 0);

    public FlyweightHeader init(final int input,
                                final int type,
                                final long sequence,
                                final long time,
                                final short index,
                                final int payloadLSize,
                                final MutableDirectBuffer dst,
                                final int dstOffset) {
        writeTo(input, sequence, index, type, time, payloadLSize, dst, dstOffset);
        return initSilent(dst, dstOffset);
    }

    public FlyweightHeader init(final Header header, final MutableDirectBuffer dst, final int dstOffset) {
        return init(header.input(), header.type(), header.sequence(), header.time(), header.index(),
                header.payloadSize(), dst, dstOffset);
    }

    @Override
    public FlyweightHeader init(final DirectBuffer src, final int srcOffset) {
        Version.validate(src.getShort(srcOffset + VERSION_OFFSET));
        return initSilent(src, srcOffset);
    }

    private FlyweightHeader initSilent(final DirectBuffer src, final int srcOffset) {
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
    public int input() {
        return header.getInt(INPUT_OFFSET);
    }

    @Override
    public int type() {
        return header.getShort(TYPE_OFFSET);
    }

    @Override
    public long sequence() {
        return header.getLong(SEQUENCE_OFFSET);
    }

    @Override
    public long time() {
        return header.getLong(TIME_OFFSET);
    }

    @Override
    public int version() {
        return 0xffff & header.getShort(VERSION_OFFSET);
    }

    @Override
    public short index() {
        return header.getShort(INDEX_OFFSET);
    }

    @Override
    public int payloadSize() {
        return header.getShort(PAYLOAD_SIZE_OFFSET);
    }

    @Override
    public int write(final BufferAcquirer bufferAcquirer) {
        final MutableDirectBuffer buffer = bufferAcquirer.acquireBuffer(HEADER_LENGTH);
        buffer.putBytes(HEADER_OFFSET, header, 0, HEADER_LENGTH);
        return HEADER_LENGTH;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        dst.putBytes(dstOffset + HEADER_OFFSET, header, 0, HEADER_LENGTH);
        return HEADER_LENGTH;
    }

    public static int writeTo(final int input,
                              final long sequence,
                              final short index,
                              final int type,
                              final long time,
                              final int payloadLSize,
                              final MutableDirectBuffer dst,
                              final int dstOffset) {
        dst.putInt(dstOffset + INPUT_OFFSET, input);
        dst.putInt(dstOffset + TYPE_OFFSET, type);
        dst.putLong(dstOffset + SEQUENCE_OFFSET, sequence);
        dst.putLong(dstOffset + TIME_OFFSET, time);
        dst.putShort(dstOffset + VERSION_OFFSET, Version.CURRENT);
        dst.putShort(dstOffset + INDEX_OFFSET, index);
        dst.putInt(dstOffset + PAYLOAD_SIZE_OFFSET, payloadLSize);
        return HEADER_LENGTH;
    }

    @Override
    public String toString() {
        return valid() ? "FlyweightHeader{" +
                "input=" + input() +
                ", type=" + type() +
                ", sequence=" + sequence() +
                ", time=" + time() +
                ", version=" + version() +
                ", index=" + index() +
                ", payload-size=" + payloadSize() +
                '}' : "FlyweightHeader";
    }
}
