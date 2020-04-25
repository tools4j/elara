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
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.log.Writable;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;

/**
 * Unit test for {@link FlyweightCommand}
 */
public class FlyweightCommandTest {

    @Test
    public void unwrapped() {
        //given
        final FlyweightCommand command = new FlyweightCommand();

        //when + then
        assertNotNull(command.payload(), "id.payload");
        assertEquals(0, command.payload().capacity(), "id.payload.capacity");
        assertFalse(command.valid(), "command.valid");

        try {
            command.input();
        } catch (final IndexOutOfBoundsException e) {
            //expected
        }
    }

    @Test
    public void invalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> new FlyweightCommand().init(
                new ExpandableArrayBuffer(HEADER_LENGTH), 0
        ));
    }

    @Test
    public void defaultValues() {
        //given
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(HEADER_LENGTH);
        buffer.putShort(FrameDescriptor.VERSION_OFFSET, Version.CURRENT);
        final FlyweightCommand command = new FlyweightCommand().init(buffer, 0);

        //when + then
        assertEquals(0, command.id().input(), "id.input");
        assertEquals(0, command.id().sequence(), "id.sequence");
        assertEquals(0, command.type(), "id.type");
        assertEquals(0, command.time(), "id.time");
        assertNotNull(command.payload(), "payload");
        assertEquals(0, command.payload().capacity(), "payload.capacity");
        assertTrue(command.valid(), "command.valid");
    }

    @Test
    public void initWithValues() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values(payloadOffset, "Hello world");
        final FlyweightCommand command = new FlyweightCommand();

        //when
        command.init(new ExpandableArrayBuffer(), headerOffset,
                values.input, values.seq, values.type, values.time, values.payload, payloadOffset, values.payloadLength()
        );

        //then
        values.assertCommand(command);

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int totalLen = command.writeTo(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), totalLen, "total bytes length");

        //when
        final FlyweightCommand copy = new FlyweightCommand().init(buffer, copyOffset);

        //then
        values.assertCommand(copy);
    }

    @Test
    public void initWithBuffer() {
        //given
        final Values values = new Values(0, "Hello world");
        final FlyweightCommand command = new FlyweightCommand().init(
                new ExpandableArrayBuffer(), 0, values.input, values.seq,
                values.type, values.time, values.payload, 0, values.payloadLength()
        );

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int totalLen = command.writeTo(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), totalLen, "total bytes length");

        //when
        final FlyweightCommand copy = new FlyweightCommand().init(buffer, copyOffset);

        //then
        values.assertCommand(copy);
    }

    @Test
    public void initWithHeaderAndPayload() {
        //given
        final Values values = new Values(0, "Hello world");
        final FlyweightCommand command = new FlyweightCommand().init(
                new ExpandableArrayBuffer(), 0, values.input, values.seq,
                values.type, values.time, values.payload, 0, values.payloadLength()
        );

        //when
        final int headerOffset = 7;
        final int payloadOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        command.writeTo(buffer, 0);
        final MutableDirectBuffer header = new ExpandableArrayBuffer();
        header.putBytes(headerOffset, buffer, 0, HEADER_LENGTH);
        final MutableDirectBuffer payload = new ExpandableArrayBuffer();
        payload.putBytes(payloadOffset, buffer, HEADER_LENGTH, values.payloadLength());

        //when
        final FlyweightCommand copy = new FlyweightCommand().init(header, headerOffset, payload, payloadOffset, values.payloadLength());

        //then
        values.assertCommand(copy);
    }

    @Test
    public void write() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values(payloadOffset, "Hello world");
        final FlyweightCommand command = new FlyweightCommand();
        command.init(new ExpandableArrayBuffer(), headerOffset,
                values.input, values.seq, values.type, values.time, values.payload, payloadOffset, values.payloadLength()
        );

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        final int writeToLen = command.writeTo(buffer, copyOffset);
        final FlyweightCommand writtenTo = new FlyweightCommand().init(buffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), writeToLen, "write-to byte length");
        values.assertCommand(writtenTo);

        //when
        final AtomicReference<MutableDirectBuffer> bufferRef = new AtomicReference<>();
        final Writable.BufferAcquirer acquirer = length -> {
            bufferRef.set(new UnsafeBuffer(ByteBuffer.allocate(length)));
            return bufferRef.get();
        };
        final int writeLen = command.write(acquirer);
        final FlyweightCommand written = new FlyweightCommand().init(bufferRef.get(), 0);

        //then
        assertEquals(HEADER_LENGTH + values.payloadLength(), bufferRef.get().capacity(), "buffer capacity (reserved length)");
        assertEquals(HEADER_LENGTH + values.payloadLength(), writeLen, "written byte length");
        values.assertCommand(written);
    }

    private static class Values {
        final int input = 77;
        final long seq = 998877;
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

        void assertCommand(final Command command) {
            assertEquals(input, command.id().input(), "id.input");
            assertEquals(seq, command.id().sequence(), "id.sequence");
            assertEquals(type, command.type(), "id.type");
            assertEquals(time, command.time(), "id.time");
            assertNotNull(command.payload(), "id.payload");
            assertEquals(payloadLength(), command.payload().capacity(), "payload.capacity");
            assertEquals(msg, command.payload().getStringAscii(0), "payload.msg");
        }
    }
}