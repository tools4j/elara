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

import static org.tools4j.elara.flyweight.HeaderDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.HeaderDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.INPUT_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.SIZE_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.TIME_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.TYPE_OFFSET;
import static org.tools4j.elara.flyweight.HeaderDescriptor.VERSION_OFFSET;

public class FlyweightCommand implements Flyweight<FlyweightCommand>, Command, Command.Id {

    private static final short INDEX_NEG = Short.MIN_VALUE;
    private final MutableDirectBuffer header = new UnsafeBuffer(0, 0);
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
        writeHeaderTo(header, headerOffset, input, sequence, type, time, payloadLSize);
        return initSlient(header, headerOffset, payload, payloadOffset, payloadLSize);
    }

    public FlyweightCommand init(final DirectBuffer header,
                                 final int headerOffset,
                                 final DirectBuffer payload,
                                 final int payloadOffset,
                                 final int payloadSize) {
        Version.validate(header.getShort(headerOffset + VERSION_OFFSET));
        return initSlient(header, headerOffset, payload, payloadOffset, payloadSize);
    }

    private FlyweightCommand initSlient(final DirectBuffer header,
                                        final int headerOffset,
                                        final DirectBuffer payload,
                                        final int payloadOffset,
                                        final int payloadSize) {
        this.header.wrap(header, headerOffset, HEADER_LENGTH);
        if (payloadSize == 0) {
            this.payload.wrap(0, 0);
        } else {
            this.payload.wrap(payload, payloadOffset, payloadSize);
        }
        return this;
    }

    @Override
    public FlyweightCommand init(final DirectBuffer command, final int offset) {
        return this.init(
                command, offset + HEADER_OFFSET,
                command, offset + PAYLOAD_OFFSET,
                command.getInt(offset + SIZE_OFFSET)
        );
    }

    public FlyweightCommand reset() {
        header.wrap(0, 0);
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Id id() {
        return this;
    }

    @Override
    public int input() {
        return header.getInt(INPUT_OFFSET);
    }

    @Override
    public long sequence() {
        return header.getLong(SEQUENCE_OFFSET);
    }

    @Override
    public int type() {
        return header.getInt(TYPE_OFFSET);
    }

    @Override
    public long time() {
        return header.getLong(TIME_OFFSET);
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    public static int writeHeaderTo(final MutableDirectBuffer header,
                                    final int headerOffset,
                                    final int input,
                                    final long sequence,
                                    final int type,
                                    final long time,
                                    final int payloadLSize) {
        header.putInt(headerOffset + INPUT_OFFSET, input);
        header.putInt(headerOffset + TYPE_OFFSET, type);
        header.putLong(headerOffset + SEQUENCE_OFFSET, sequence);
        header.putLong(headerOffset + TIME_OFFSET, time);
        header.putShort(headerOffset + VERSION_OFFSET, Version.CURRENT);
        header.putShort(headerOffset + INDEX_OFFSET, INDEX_NEG);
        header.putInt(headerOffset + SIZE_OFFSET, payloadLSize);
        return HEADER_LENGTH;
    }

    @Override
    public int writeTo(final MutableDirectBuffer buffer, final int offset) {
        buffer.putBytes(offset + HEADER_OFFSET, header, 0, HEADER_LENGTH);
        buffer.putBytes(offset + PAYLOAD_OFFSET, payload, 0, payload.capacity());
        return HEADER_LENGTH + payload.capacity();
    }

    @Override
    public String toString() {
        if (header.capacity() < HEADER_LENGTH) {
            return "FlyweightCommand";
        }
        return "FlyweightCommand{" +
                "input=" + header.getInt(INPUT_OFFSET) +
                ", type=" + header.getInt(TYPE_OFFSET) +
                ", sequence=" + header.getLong(SEQUENCE_OFFSET) +
                ", time=" + header.getLong(TIME_OFFSET) +
                ", version=" + header.getShort(VERSION_OFFSET) +
                ", index=" + header.getShort(INDEX_OFFSET) +
                ", payload-size=" + header.getInt(SIZE_OFFSET) +
                '}';
    }
}
