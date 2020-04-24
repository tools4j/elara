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
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;

public class FlyweightDataFrame implements Flyweight<FlyweightDataFrame>, DataFrame {

    private final FlyweightHeader header = new FlyweightHeader();
    private final DirectBuffer payload = new UnsafeBuffer(0, 0);

    public FlyweightDataFrame init(final MutableDirectBuffer header,
                                   final int headerOffset,
                                   final int input,
                                   final int type,
                                   final long sequence,
                                   final long time,
                                   final short index,
                                   final DirectBuffer payload,
                                   final int payloadOffset,
                                   final int payloadSize) {
        this.header.init(input, type, sequence, time, index, payloadSize, header, headerOffset);
        return initPayload(payload, payloadOffset, payloadSize);
    }

    public FlyweightDataFrame init(final DirectBuffer header,
                                   final int headerOffset,
                                   final DirectBuffer payload,
                                   final int payloadOffset,
                                   final int payloadSize) {
        this.header.init(header, headerOffset);
        return initPayload(payload, payloadOffset, payloadSize);
    }

    @Override
    public FlyweightDataFrame init(final DirectBuffer event, final int offset) {
        return this.init(
                event, offset + HEADER_OFFSET,
                event, offset + PAYLOAD_OFFSET,
                event.getInt(offset + PAYLOAD_SIZE_OFFSET)
        );
    }

    private FlyweightDataFrame initPayload(final DirectBuffer payload,
                                           final int payloadOffset,
                                           final int payloadSize) {
        if (payloadSize == 0) {
            this.payload.wrap(0, 0);
        } else {
            this.payload.wrap(payload, payloadOffset, payloadSize);
        }
        return this;
    }

    public boolean valid() {
        return header.valid();
    }

    public FlyweightDataFrame reset() {
        header.reset();
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    @Override
    public int write(final BufferAcquirer bufferAcquirer) {
        final int payloadSize = payload.capacity();
        final MutableDirectBuffer dst = bufferAcquirer.acquireBuffer(HEADER_LENGTH + payloadSize);
        header.writeTo(dst, 0);
        payload.getBytes(0, dst, PAYLOAD_OFFSET, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int offset) {
        final int payloadSize = payload.capacity();
        header.writeTo(dst, offset);
        payload.getBytes(0, dst, offset + PAYLOAD_OFFSET, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    @Override
    public String toString() {
        return valid() ? "FlyweightDataFrame{" +
                "input=" + header().input() +
                ", type=" + header().type() +
                ", sequence=" + header().sequence() +
                ", time=" + header().time() +
                ", version=" + header().version() +
                ", index=" + header().index() +
                ", payload-size=" + header().payloadSize() +
                '}' : "FlyweightDataFrame";
    }
}
