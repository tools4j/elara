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

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.Test;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.tools4j.elara.flyweight.HeaderDescriptor.HEADER_LENGTH;

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

        try {
            event.input();
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
        buffer.putShort(HeaderDescriptor.VERSION_OFFSET, Version.CURRENT);
        final FlyweightEvent event = new FlyweightEvent().init(buffer, 0);

        //when + then
        assertEquals(0, event.id().commandId().input(), "id.commandId.input");
        assertEquals(0, event.id().commandId().sequence(), "id.commandId,sequence");
        assertEquals(0, event.id().index(), "id.index");
        assertEquals(0, event.type(), "id.type");
        assertEquals(0, event.time(), "id.time");
        assertNotNull(event.payload(), "payload");
        assertEquals(0, event.payload().capacity(), "payload.capacity");
    }

    @Test
    public void initWithValues() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values(payloadOffset, "Hello world");
        final FlyweightEvent event = new FlyweightEvent();

        //when
        event.init(new ExpandableArrayBuffer(), headerOffset, values.input, values.seq, values.index,
                values.type, values.time, values.payload, payloadOffset, values.payloadLength()
        );

        //when + then
        assertEquals(values.input, event.id().commandId().input(), "id..commandId.input");
        assertEquals(values.seq, event.id().commandId().sequence(), "id.commandId.sequence");
        assertEquals(values.index, event.id().index(), "id.index");
        assertEquals(values.type, event.type(), "id.type");
        assertEquals(values.time, event.time(), "id.time");
        assertNotNull(event.payload(), "id.payload");
        assertEquals(values.payloadLength(), event.payload().capacity(), "payload.capacity");
        assertEquals(values.msg, event.payload().getStringAscii(0), "payload.msg");

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int totalLen = event.writeTo(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), totalLen, "total bytes length");

        //when
        final FlyweightEvent copy = new FlyweightEvent().init(buffer, copyOffset);

        //then
        assertEquals(values.input, event.id().commandId().input(), "id..commandId.input");
        assertEquals(values.seq, event.id().commandId().sequence(), "id.commandId.sequence");
        assertEquals(values.index, event.id().index(), "id.index");
        assertEquals(values.type, copy.type(), "id.type");
        assertEquals(values.time, copy.time(), "id.time");
        assertNotNull(copy.payload(), "id.payload");
        assertEquals(values.payloadLength(), copy.payload().capacity(), "payload.capacity");
        assertEquals(values.msg, copy.payload().getStringAscii(0), "payload.msg");
    }

    @Test
    public void initWithBuffer() {
        //given
        final Values values = new Values(0, "Hello world");
        final FlyweightEvent event = new FlyweightEvent().init(
                new ExpandableArrayBuffer(), 0, values.input, values.seq, values.index,
                values.type, values.time, values.payload, 0, values.payloadLength()
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
        assertEquals(values.input, event.id().commandId().input(), "id..commandId.input");
        assertEquals(values.seq, event.id().commandId().sequence(), "id.commandId.sequence");
        assertEquals(values.index, event.id().index(), "id.index");
        assertEquals(values.type, copy.type(), "id.type");
        assertEquals(values.time, copy.time(), "id.time");
        assertNotNull(copy.payload(), "id.payload");
        assertEquals(values.payloadLength(), copy.payload().capacity(), "payload.capacity");
        assertEquals(values.msg, copy.payload().getStringAscii(0), "payload.msg");
    }

    @Test
    public void initWithHeaderAndPayload() {
        //given
        final Values values = new Values(0, "Hello world");
        final FlyweightEvent event = new FlyweightEvent().init(
                new ExpandableArrayBuffer(), 0, values.input, values.seq, values.index,
                values.type, values.time, values.payload, 0, values.payloadLength()
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
        assertEquals(values.input, event.id().commandId().input(), "id..commandId.input");
        assertEquals(values.seq, event.id().commandId().sequence(), "id.commandId.sequence");
        assertEquals(values.index, event.id().index(), "id.index");
        assertEquals(values.type, copy.type(), "id.type");
        assertEquals(values.time, copy.time(), "id.time");
        assertNotNull(copy.payload(), "id.payload");
        assertEquals(values.payloadLength(), copy.payload().capacity(), "payload.capacity");
        assertEquals(values.msg, copy.payload().getStringAscii(0), "payload.msg");
    }

    private static class Values {
        final int input = 77;
        final long seq = 998877;
        final short index = 7;
        final int type = 123;
        final long time = 998877665544L;
        final String msg;
        final MutableDirectBuffer payload = new ExpandableArrayBuffer();

        Values(final int payloadOffset, final String msg) {
            this.msg = requireNonNull(msg);
            payload.putStringAscii(payloadOffset, msg);
        }

        int payloadLength() {
            return Integer.BYTES + msg.length();
        }
    }
}