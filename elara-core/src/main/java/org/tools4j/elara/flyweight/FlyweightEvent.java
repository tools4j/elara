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
import static org.tools4j.elara.flyweight.EventDescriptor.EVENT_INDEX_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.EVENT_SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.EVENT_TIME_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.PAYLOAD_TYPE_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.SOURCE_ID_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.SOURCE_SEQUENCE_OFFSET;

/**
 * A flyweight event for reading and writing event data laid out as per {@link EventDescriptor} definition.
 */
public class FlyweightEvent implements Flyweight<FlyweightEvent>, Event, EventFrame {
    public static final int HEADER_LENGTH = EventDescriptor.HEADER_LENGTH;

    private final FlyweightHeader header = new FlyweightHeader(HEADER_LENGTH);
    private final MutableDirectBuffer payload = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightEvent wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        FrameType.validateEventType(header.type());
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
    public int eventIndex() {
        return eventIndex(header.buffer());
    }

    public static int eventIndex(final DirectBuffer buffer) {
        return 0x7fff & buffer.getShort(EVENT_INDEX_OFFSET, LITTLE_ENDIAN);
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
    public EventType eventType() {
        return EventType.valueByFrameType(header.type());
    }

    @Override
    public DirectBuffer payload() {
        return payload;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        final int payloadSize = payload.capacity();
        writeHeader(eventType(), sourceId(), sourceSequence(), (short) eventIndex(), eventSequence(),
                eventTime(), payloadType(), payloadSize, dst, dstOffset);
        dst.putBytes(dstOffset + PAYLOAD_OFFSET, payload, 0, payloadSize);
        return HEADER_LENGTH + payloadSize;
    }

    public static int writeHeader(final EventType eventType,
                                  final int sourceId,
                                  final long sourceSequence,
                                  final short eventIndex,
                                  final long eventSequence,
                                  final long eventTime,
                                  final int payloadType,
                                  final int payloadSize,
                                  final MutableDirectBuffer dst,
                                  final int dstOffset) {
        assert eventIndex >= 0;
        final int frameSize = HEADER_LENGTH + payloadSize;
        FlyweightHeader.write(eventType.frameType(), eventIndex, frameSize, dst, dstOffset);
        dst.putLong(dstOffset + SOURCE_ID_OFFSET,
                (0xffffffffL & sourceId) | ((0xffffffffL & payloadType) << 32),
                LITTLE_ENDIAN);
        dst.putLong(dstOffset + SOURCE_SEQUENCE_OFFSET, sourceSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + EVENT_SEQUENCE_OFFSET, eventSequence, LITTLE_ENDIAN);
        dst.putLong(dstOffset + EVENT_TIME_OFFSET, eventTime, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static void writeEventType(final EventType eventType, final MutableDirectBuffer dst) {
        FlyweightHeader.writeType(eventType.frameType(), dst);
    }

    public static void writePayloadSize(final int payloadSize, final MutableDirectBuffer dst) {
        assert payloadSize >= 0;
        FlyweightHeader.writeFrameSize(HEADER_LENGTH + payloadSize, dst);
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
            dst.append("|event-index=").append(eventIndex());
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
