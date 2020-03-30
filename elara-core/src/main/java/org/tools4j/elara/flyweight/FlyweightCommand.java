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
import org.tools4j.elara.command.Command;
import org.tools4j.elara.log.Flyweight;

import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;

public class FlyweightCommand implements Flyweight<FlyweightCommand>, Command, Command.Id, Frame {

    private static final short INDEX_NEG = Short.MIN_VALUE;
    private final FlyweightHeader header = new FlyweightHeader();
    private final DirectBuffer payload = new UnsafeBuffer(0, 0);

    public FlyweightCommand init(final MutableDirectBuffer header,
                                 final int headerOffset,
                                 final int input,
                                 final long sequence,
                                 final int type,
                                 final long time,
                                 final DirectBuffer payload,
                                 final int payloadOffset,
                                 final int payloadLSize) {
        this.header.init(input, type, sequence, time, INDEX_NEG, payloadLSize, header, headerOffset);
        return initPayload(payload, payloadOffset, payloadLSize);
    }

    public FlyweightCommand init(final DirectBuffer header,
                                 final int headerOffset,
                                 final DirectBuffer payload,
                                 final int payloadOffset,
                                 final int payloadSize) {
        this.header.init(header, headerOffset);
        return initPayload(payload, payloadOffset, payloadSize);
    }

    @Override
    public FlyweightCommand init(final DirectBuffer command, final int offset) {
        return this.init(
                command, offset + HEADER_OFFSET,
                command, offset + PAYLOAD_OFFSET,
                command.getInt(offset + PAYLOAD_SIZE_OFFSET)
        );
    }

    private FlyweightCommand initPayload(final DirectBuffer payload,
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

    public FlyweightCommand reset() {
        header.reset();
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public Id id() {
        return this;
    }

    @Override
    public int input() {
        return header.input();
    }

    @Override
    public long sequence() {
        return header.sequence();
    }

    @Override
    public int type() {
        return header.type();
    }

    @Override
    public long time() {
        return header.time();
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    @Override
    public int writeTo(final MutableDirectBuffer buffer, final int offset) {
        header.writeTo(buffer, offset);
        payload.getBytes(0, buffer, offset + PAYLOAD_OFFSET, payload.capacity());
        return HEADER_LENGTH + payload.capacity();
    }

    @Override
    public String toString() {
        return valid() ? "FlyweightCommand{" +
                "input=" + input() +
                ", type=" + type() +
                ", sequence=" + sequence() +
                ", time=" + time() +
                ", version=" + header.version() +
                ", index=" + header.index() +
                ", payload-size=" + header.payloadSize() +
                '}' : "FlyweightCommand";
    }
}
