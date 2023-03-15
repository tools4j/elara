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
import org.tools4j.elara.event.Event;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.EventDescriptor.EVENT_SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.EVENT_TIME_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.PAYLOAD_TYPE_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.SOURCE_ID_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.SOURCE_SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.FrameType.EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.NIL_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.ROLLBACK_EVENT_TYPE;

public class FlyweightEvent implements Flyweight<FlyweightEvent>, Event, EventFrame {
    public static final int HEADER_LENGTH = EventDescriptor.HEADER_LENGTH;

    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final MutableDirectBuffer payload = new UnsafeBuffer(0, 0);
    private final Flags flags = new FlyweightFlags(this);

    @Override
    public FlyweightEvent wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        return wrapPayload(buffer, offset);
    }

    public FlyweightEvent wrapSilently(final DirectBuffer buffer, final int offset) {
        header.wrapSilently(buffer, offset);
        return wrapPayload(buffer, offset);
    }

    private FlyweightEvent wrapPayload(final DirectBuffer buffer, final int offset) {
        final int frameSize = header.frameSize();
        payload.wrap(buffer, offset + HEADER_LENGTH, frameSize - HEADER_LENGTH);
        return this;
    }

    @Override
    public boolean valid() {
        return header.valid();
    }

    public FlyweightEvent reset() {
        header.reset();
        payload.wrap(0, 0);
        return this;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public int sourceId() {
        return sourceId(header.buffer());
    }

    public static int sourceId(final DirectBuffer buffer) {
        return buffer.getInt(SOURCE_ID_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public long sourceSequence() {
        return sourceSequence(header.buffer());
    }

    public static long sourceSequence(final DirectBuffer buffer) {
        return buffer.getLong(SOURCE_SEQUENCE_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int index() {
        return index(header.buffer());
    }

    public static int index(final DirectBuffer buffer) {
        return 0x7fff & buffer.getShort(INDEX_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public boolean last() {
        return last(header.buffer());
    }

    public static boolean last(final DirectBuffer buffer) {
        return (0x8000 & buffer.getShort(INDEX_OFFSET, LITTLE_ENDIAN)) != 0;
    }


    @Override
    public long eventSequence() {
        return eventSequence(header.buffer());
    }

    public static long eventSequence(final DirectBuffer buffer) {
        return buffer.getLong(EVENT_SEQUENCE_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int payloadType() {
        return payloadType(header.buffer());
    }

    public static int payloadType(final DirectBuffer buffer) {
        return buffer.getInt(PAYLOAD_TYPE_OFFSET);
    }

    @Override
    public long eventTime() {
        return eventTime(header.buffer());
    }

    public static long eventTime(final DirectBuffer buffer) {
        return buffer.getLong(EVENT_TIME_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public Flags flags() {
        return flags;
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        final int payloadSize = payload.capacity();
        writeHeader(sourceId(), sourceSequence(), index(), flags().isLast(), eventSequence(),
                eventTime(), payloadType(), payloadSize, dst, dstOffset);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, 0, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    public static int writeHeader(final int sourceId,
                                  final long sourceSequence,
                                  final int index,
                                  final boolean last,
                                  final long eventSequence,
                                  final long eventTime,
                                  final int payloadType,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        return writeHeader(EVENT_TYPE, sourceId, sourceSequence, index, last, eventSequence, eventTime, payloadType,
                payloadSize, dst, dstOffset);
    }

    public static int writeHeader(final byte eventType,
                                  final int sourceId,
                                  final long sourceSequence,
                                  final int index,
                                  final boolean last,
                                  final long eventSequence,
                                  final long eventTime,
                                  final int payloadType,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        assert eventType == EVENT_TYPE || eventType == NIL_EVENT_TYPE || eventType == ROLLBACK_EVENT_TYPE;
        assert index >= 0 && index <= Short.MAX_VALUE;
        final int frameSize = HEADER_LENGTH + payloadSize;
        final short flagAndIndex = (short)((last ? 0x8000: 0x0) | (0x7fff & index));
        FlyweightHeader.write(eventType, flagAndIndex, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + SOURCE_ID_OFFSET,
                (0xffffffffL & sourceId) | ((0xffffffffL & payloadType) << 32),
                LITTLE_ENDIAN);
        dst.putLong(dstOffset + SOURCE_SEQUENCE_OFFSET, sourceSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + EVENT_SEQUENCE_OFFSET, eventSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + EVENT_TIME_OFFSET, eventTime, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static void last(final boolean last, final MutableDirectBuffer dst) {
        final int index = FlyweightEvent.index(dst);
        final short flagAndIndex = (short)(last ? (0x8000 | index) : index);
        dst.putShort(INDEX_OFFSET, flagAndIndex, LITTLE_ENDIAN);
    }

    public static void payloadSize(final int payloadSize, final MutableDirectBuffer dst) {
        FlyweightHeader.frameSize(HEADER_LENGTH + payloadSize, dst);
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightEvent{");
        if (valid()) {
            final Header header = header();
            dst.append("version=").append(header.version());
            dst.append("|type=").append(type());
            dst.append("|frame-size=").append(frameSize());
            dst.append("|source-id=").append(sourceId());
            dst.append("|source-seq=").append(sourceSequence());
            dst.append("|index=").append(index());
            dst.append("|last=").append(flags.isLast());
            dst.append("|event-seq=").append(eventSequence());
            dst.append("|event-time=").append(eventTime());
            dst.append("|payload-type=").append(payloadType());
            dst.append("|payload-size=").append(payloadSize());
        } else {
            dst.append("???");
        }
        dst.append('}');
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }

}
