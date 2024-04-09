/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.app.message.Command;

import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.flyweight.FlyweightCommand.HEADER_LENGTH;

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
            command.sourceId();
        } catch (final IndexOutOfBoundsException e) {
            //expected
        }
    }

    @Test
    public void invalidVersion() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new FlyweightCommand().wrap(
                new ExpandableArrayBuffer(HEADER_LENGTH), 0
        ));
    }

    @Test
    public void defaultValues() {
        //given
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(HEADER_LENGTH);
        buffer.putByte(FrameDescriptor.VERSION_OFFSET, Version.CURRENT);
        buffer.putByte(FrameDescriptor.TYPE_OFFSET, FrameType.COMMAND_TYPE);
        buffer.putInt(FrameDescriptor.FRAME_SIZE_OFFSET, FlyweightCommand.HEADER_LENGTH);
        final FlyweightCommand command = new FlyweightCommand().wrap(buffer, 0);

        //when + then
        assertEquals(0, command.sourceId(), "sourceId");
        assertEquals(0, command.sourceSequence(), "sourceSequence");
        assertEquals(0, command.payloadType(), "payloadType");
        assertEquals(0, command.commandTime(), "commandTime");
        assertNotNull(command.payload(), "payload");
        assertEquals(0, command.payload().capacity(), "payload.capacity");
        assertTrue(command.valid(), "command.valid");
    }

    @Test
    public void write() {
        //given
        final int headerOffset = 23;
        final int payloadOffset = 13;
        final Values values = new Values(payloadOffset, "Hello world");
        final FlyweightCommand command = new FlyweightCommand();
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(headerOffset + HEADER_LENGTH + values.payloadSize());

        //when
        final int written = FlyweightCommand.writeHeader(values.reserved, values.sourceId, values.sourceSeq,
                values.commandTime, values.payloadType, values.payloadSize(), buffer, headerOffset);
        command.wrap(buffer, headerOffset);

        //then
        values.assertHeader(command);
        assertEquals(HEADER_LENGTH, written, "bytes written");

        //when
        final int copyOffset = 7;
        final MutableDirectBuffer copyBuffer = new ExpandableArrayBuffer();
        final int copyLen = command.writeTo(copyBuffer, copyOffset);

        //then
        assertEquals(HEADER_LENGTH + values.payloadSize(), copyLen, "bytes copied");

        //when
        final FlyweightCommand copy = new FlyweightCommand().wrap(copyBuffer, copyOffset);

        //then
        values.assertHeader(copy);
    }

    private static class Values {
        final short reserved = 234;
        final int sourceId = 77;
        final long sourceSeq = 998877;
        final long commandTime = 998877665544L;
        final int payloadType = 125688;
        final String msg;
        final MutableDirectBuffer payload = new ExpandableArrayBuffer();

        Values(final int payloadOffset, final String msg) {
            this.msg = requireNonNull(msg);
            payload.putStringAscii(payloadOffset, msg);
        }

        int payloadSize() {
            return Integer.BYTES + msg.length();
        }

        void assertHeader(final Command command) {
            assertEquals(reserved, ((DataFrame)command).header().reserved(), "header.reserved");
            assertEquals(sourceId, command.sourceId(), "sourceId");
            assertEquals(sourceSeq, command.sourceSequence(), "sourceSequence");
            assertEquals(payloadType, command.payloadType(), "payloadType");
            assertEquals(commandTime, command.commandTime(), "commandTime");
            assertNotNull(command.payload(), "payload");
            assertEquals(payloadSize(), command.payload().capacity(), "payload.capacity");
        }
        void assertHeaderAndPayload(final Command command) {
            assertHeader(command);
            assertEquals(msg, command.payload().getStringAscii(0), "payload.msg");
        }
    }
}