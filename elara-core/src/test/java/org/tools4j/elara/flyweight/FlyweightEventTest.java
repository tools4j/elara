/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;

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

        try {
            event.source();
        } catch (final IndexOutOfBoundsException e) {
            //expected
        }
    }

    @Test
    public void invalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> new FlyweightEvent().init(
                new ExpandableArrayBuffer(HEADER_LENGTH), 0
        ));
    }

    @Test
    public void defaultValues() {
        //given
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(HEADER_LENGTH);
        buffer.putShort(FrameDescriptor.VERSION_OFFSET, Version.CURRENT);
        final FlyweightEvent event = new FlyweightEvent().init(buffer, 0);

        //when + then
        assertEquals(0, event.id().commandId().source(), "id.commandId.source");
        assertEquals(0, event.id().commandId().sequence(), "id.commandId,sequence");
        assertEquals(0, event.id().index(), "id.index");
        assertEquals(0, event.type(), "id.type");
        assertEquals(0, event.time(), "id.time");
        assertNotNull(event.payload(), "payload");
        assertEquals(0, event.payload().capacity(), "payload.capacity");
        assertTrue(event.valid(), "event.valid");
    }

    @Test
    public void initWithValues() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values(payloadOffset, "Hello world");
        final FlyweightEvent event = new FlyweightEvent();

        //when
        event.init(new ExpandableArrayBuffer(), headerOffset, values.source, values.seq, values.index,
                values.type, values.time, values.flags, values.payload, payloadOffset, values.payloadLength()
        );

        //then
        values.assertEvent(event);

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int totalLen = event.writeTo(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), totalLen, "total bytes length");

        //when
        final FlyweightEvent copy = new FlyweightEvent().init(buffer, copyOffset);

        //then
        values.assertEvent(copy);
    }

    @Test
    public void initWithBuffer() {
        //given
        final Values values = new Values(0, "Hello world");
        final FlyweightEvent event = new FlyweightEvent().init(
                new ExpandableArrayBuffer(), 0, values.source, values.seq, values.index,
                values.type, values.time, values.flags, values.payload, 0, values.payloadLength()
        );

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int totalLen = event.writeTo(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), totalLen, "total bytes length");

        //when
        final FlyweightEvent copy = new FlyweightEvent().init(buffer, copyOffset);

        //then
        values.assertEvent(copy);
    }

    @Test
    public void initWithHeaderAndPayload() {
        //given
        final Values values = new Values(0, "Hello world");
        final FlyweightEvent event = new FlyweightEvent().init(
                new ExpandableArrayBuffer(), 0, values.source, values.seq, values.index,
                values.type, values.time, values.flags, values.payload, 0, values.payloadLength()
        );

        //when
        final int headerOffset = 7;
        final int payloadOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        event.writeTo(buffer, 0);
        final MutableDirectBuffer header = new ExpandableArrayBuffer();
        header.putBytes(headerOffset, buffer, 0, HEADER_LENGTH);
        final MutableDirectBuffer payload = new ExpandableArrayBuffer();
        payload.putBytes(payloadOffset, buffer, HEADER_LENGTH, values.payloadLength());

        //when
        final FlyweightEvent copy = new FlyweightEvent().init(header, headerOffset, payload, payloadOffset, values.payloadLength());

        //then
        values.assertEvent(copy);
    }

    @Test
    public void write() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values(payloadOffset, "Hello world");
        final FlyweightEvent event = new FlyweightEvent();
        event.init(new ExpandableArrayBuffer(), headerOffset, values.source, values.seq, values.index,
                values.type, values.time, values.flags, values.payload, payloadOffset, values.payloadLength()
        );

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int writeToLen = event.writeTo(buffer, copyOffset);
        final FlyweightEvent writtenTo = new FlyweightEvent().init(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), writeToLen, "write-to length");
        values.assertEvent(writtenTo);
    }

    private static class Values {
        final int source = 77;
        final long seq = 998877;
        final short index = 7;
        final int type = 123;
        final long time = 998877665544L;
        final byte flags = Flags.COMMIT;
        final String msg;
        final MutableDirectBuffer payload = new ExpandableArrayBuffer();

        Values(final int payloadOffset, final String msg) {
            this.msg = requireNonNull(msg);
            payload.putStringAscii(payloadOffset, msg);
        }

        int payloadLength() {
            return Integer.BYTES + msg.length();
        }

        void assertEvent(final Event event) {
            assertEquals(source, event.id().commandId().source(), "id..commandId.source");
            assertEquals(seq, event.id().commandId().sequence(), "id.commandId.sequence");
            assertEquals(index, event.id().index(), "id.index");
            assertEquals(type, event.type(), "type");
            assertEquals(time, event.time(), "time");
            assertEquals(Flags.COMMIT_STRING, event.flags().toString(), "flags");
            assertNotNull(event.payload(), "payload");
            assertEquals(payloadLength(), event.payload().capacity(), "payload.capacity");
            assertEquals(msg, event.payload().getStringAscii(0), "payload.msg");
        }
    }
}