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
package org.tools4j.elara.event;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.log.Flyweight;

public class FlyweightEvent implements Flyweight<FlyweightEvent>, Event, Event.Id, Command.Id {

    public static final int INPUT_OFFSET = 0;
    public static final int INPUT_LENGTH = Integer.BYTES;
    public static final int SEQUENCE_OFFSET = INPUT_OFFSET + INPUT_LENGTH;
    public static final int SEQUENCE_LENGTH = Long.BYTES;
    public static final int INDEX_OFFSET = SEQUENCE_OFFSET + SEQUENCE_LENGTH;
    public static final int INDEX_LENGTH = Integer.BYTES;
    public static final int TYPE_OFFSET = INDEX_OFFSET + INDEX_LENGTH;
    public static final int TYPE_LENGTH = Integer.BYTES;
    public static final int TIME_OFFSET = TYPE_OFFSET + TYPE_LENGTH;
    public static final int TIME_LENGTH = Long.BYTES;
    public static final int SIZE_OFFSET = TIME_OFFSET + TIME_LENGTH;
    public static final int SIZE_LENGTH = Integer.BYTES;

    public static final int HEADER_OFFSET = 0;
    public static final int HEADER_LENGTH = INPUT_LENGTH + SEQUENCE_LENGTH +
            INDEX_LENGTH + TYPE_LENGTH + TIME_LENGTH + SIZE_LENGTH;
    public static final int PAYLOAD_OFFSET = HEADER_OFFSET + HEADER_LENGTH;

    private final MutableDirectBuffer header = new UnsafeBuffer(0, 0);
    private final DirectBuffer payload = new UnsafeBuffer(0, 0);

    public FlyweightEvent init(final MutableDirectBuffer header,
                               final int headerOffset,
                               final int input,
                               final long sequence,
                               final int index,
                               final int type,
                               final long time,
                               final DirectBuffer payload,
                               final int payloadOffset,
                               final int payloadLSize) {
        writeHeaderTo(header, headerOffset, input, sequence, index, type, time, payloadLSize);
        return init(header, headerOffset, payload, payloadOffset, payloadLSize);
    }

    public FlyweightEvent init(final DirectBuffer header,
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
    public FlyweightEvent init(final DirectBuffer event, final int offset) {
        return this.init(
                event, offset + HEADER_OFFSET,
                event, offset + PAYLOAD_OFFSET,
                event.getInt(offset + SIZE_OFFSET)
        );
    }

    public FlyweightEvent reset() {
        header.wrap(0, 0);
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Id id() {
        return this;
    }

    @Override
    public Command.Id commandId() {
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
    public int index() {
        return header.getInt(INDEX_OFFSET);
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
                                    final int index,
                                    final int type,
                                    final long time,
                                    final int payloadLSize) {
        header.putInt(headerOffset + INPUT_OFFSET, input);
        header.putLong(headerOffset + SEQUENCE_OFFSET, sequence);
        header.putInt(headerOffset + INDEX_OFFSET, index);
        header.putInt(headerOffset + TYPE_OFFSET, type);
        header.putLong(headerOffset + TIME_OFFSET, time);
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
            return "FlyweightEvent";
        }
        return "FlyweightEvent{" +
                "input=" + header.getInt(INPUT_OFFSET) +
                ", sequence=" + header.getLong(SEQUENCE_OFFSET) +
                ", index=" + header.getInt(INDEX_OFFSET) +
                ", type=" + header.getInt(TYPE_OFFSET) +
                ", time=" + header.getLong(TIME_OFFSET) +
                ", payload-size=" + header.getInt(SIZE_OFFSET) +
                '}';
    }
}
