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

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.event.Event;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.flyweight.FlyweightEvent.HEADER_LENGTH;

/**
 * Unit test for {@link FlyweightEvent}
 */
public class FlyweightEventTest {

    @Test
    public void unwrapped() {
        //given
        final FlyweightEvent event = new FlyweightEvent();

        //when + then
        assertNotNull(event.payload(), "id.payload");
        assertEquals(0, event.payload().capacity(), "id.payload.capacity");
        assertFalse(event.valid(), "event.valid");
        assertThrowsExactly(IndexOutOfBoundsException.class, event::type, "event.type");
    }

    @Test
    public void invalidVersion() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new FlyweightEvent().wrap(
                new ExpandableArrayBuffer(HEADER_LENGTH), 0
        ));
    }

    @Test
    public void defaultValues() {
        //given
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(HEADER_LENGTH);
        buffer.putByte(FrameDescriptor.VERSION_OFFSET, Version.CURRENT);
        buffer.putByte(FrameDescriptor.TYPE_OFFSET, FrameType.COMMIT_EVENT_TYPE);
        buffer.putInt(FrameDescriptor.FRAME_SIZE_OFFSET, HEADER_LENGTH);
        final FlyweightEvent event = new FlyweightEvent().wrap(buffer, 0);

        //when + then
        assertEquals(0, event.sourceId(), "sourceId");
        assertEquals(0, event.sourceSequence(), "sourceSequence");
        assertEquals(0, event.eventIndex(), "index");
        assertEquals(0, event.payloadType(), "payloadType");
        assertEquals(0, event.eventTime(), "eventTime");
        assertNotNull(event.payload(), "payload");
        assertEquals(0, event.payload().capacity(), "payload.capacity");
        assertTrue(event.valid(), "event.valid");
    }

    @Test
    public void write() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 42;
        final Values values = new Values(payloadOffset, "Hello World");
        final FlyweightEvent event = new FlyweightEvent();
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(headerOffset + HEADER_LENGTH + values.payloadSize());

        //when
        final int written = FlyweightEvent.writeHeader(values.eventType, values.sourceId, values.sourceSeq, values.index,
                values.eventSeq, values.eventTime, values.payloadType, values.payloadSize(), buffer, headerOffset);
        event.wrap(buffer, headerOffset);

        //then
        values.assertHeader(event);
        assertEquals(HEADER_LENGTH, written, "bytes written");

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer copyBuffer = new ExpandableArrayBuffer();
        final int copyLen = event.writeTo(copyBuffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadSize(), copyLen, "bytes copied");

        //when
        final FlyweightEvent copy = new FlyweightEvent().wrap(copyBuffer, copyOffset);

        //then
        values.assertHeader(copy);

        //when
        final FlyweightEvent silent = new FlyweightEvent().wrapSilently(copyBuffer, copyOffset);

        //then
        values.assertHeader(silent);
    }

    private static class Values {
        final EventType eventType = EventType.APP_COMMIT;
        final int sourceId = 77;
        final long sourceSeq = 998877000111000L;
        final short index = 7;
        final long eventSeq = 777666000000001L;
        final int payloadType = 12345;
        final long eventTime = 998877665544L;
        final String msg;
        final MutableDirectBuffer payload = new ExpandableArrayBuffer();

        Values(final int payloadOffset, final String msg) {
            this.msg = requireNonNull(msg);
            payload.putStringAscii(payloadOffset, msg);
        }

        int payloadSize() {
            return Integer.BYTES + msg.length();
        }

        void assertHeader(final Event event) {
            assertEquals(eventType, event.eventType(), "eventType");
            assertEquals(sourceId, event.sourceId(), "sourceId");
            assertEquals(sourceSeq, event.sourceSequence(), "sourceSequence");
            assertEquals(index, event.eventIndex(), "index");
            assertEquals(payloadType, event.payloadType(), "payloadType");
            assertEquals(eventTime, event.eventTime(), "eventTime");
            assertNotNull(event.payload(), "payload");
            assertEquals(payloadSize(), event.payload().capacity(), "payload.capacity");
        }

        void assertHeaderAndPayload(final Event event) {
            assertHeader(event);
            assertEquals(msg, event.payload().getStringAscii(0), "payload.msg");
        }
    }
}