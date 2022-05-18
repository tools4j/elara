/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.handler;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.send.CommandHandlingSender;
import org.tools4j.elara.send.DefaultSenderSupplier;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.time.TimeSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CommandHandlingSender}
 */
@ExtendWith(MockitoExtension.class)
public class CommandHandlingSenderTest {

    private static final int INITIAL_BUFFER_CAPACITY = 1024;

    @Mock
    private TimeSource timeSource;

    private List<Command> commandStore;

    //under test
    private SenderSupplier senderSupplier;

    @BeforeEach
    public void init() {
        commandStore = new ArrayList<>();
        senderSupplier = new DefaultSenderSupplier(new CommandHandlingSender(INITIAL_BUFFER_CAPACITY, timeSource, command -> {
            final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer();
            command.writeTo(buffer, 0);
            commandStore.add(new FlyweightCommand().init(buffer, 0));
            return Result.POLL;
        }));
    }

    @Test
    public void shouldAppendDefaultTypeCommand() {
        //given
        final long commandTime = 9988776600001L;
        final int source = 1;
        final long seq = 22;
        final String text = "Hello world!!!";
        final int offset = 77;
        final DirectBuffer message = message(text, offset);
        final int length = message.capacity() - offset;

        //when
        when(timeSource.currentTime()).thenReturn(commandTime);
        senderSupplier.senderFor(source, seq).sendCommand(message, offset, length);

        //then
        assertEquals(1, commandStore.size(), "commandStore.size");
        assertCommand(source, seq, commandTime, EventType.APPLICATION, text, commandStore.get(0));
    }

    @Test
    public void shouldAppendCommandWithType() {
        //given
        final long commandTime = 9988776600001L;
        final int source = 1;
        final long seq = 22;
        final int type = 12345;
        final String text = "Hello world!!!";
        final int offset = 77;
        final DirectBuffer message = message(text, offset);
        final int length = message.capacity() - offset;

        //when
        when(timeSource.currentTime()).thenReturn(commandTime);
        senderSupplier.senderFor(source, seq).sendCommand(type, message, offset, length);

        //then
        assertCommand(source, seq, commandTime, type, text, commandStore.get(0));
    }

    @Test
    public void shouldAppendCommandWithAppendingContext() {
        //given
        final long commandTime = 9988776600001L;
        final int source = 1;
        final long seq = 22;
        final int type = 12345;
        final String text = "Hello world!!!";
        final int offset = 77;
        final DirectBuffer message = message(text, offset);
        final int length = message.capacity() - offset;

        //when
        when(timeSource.currentTime()).thenReturn(commandTime);
        senderSupplier.senderFor(source, seq).sendCommand(type, message, offset, length);

        //then
        assertCommand(source, seq, commandTime, type, text, commandStore.get(0));
    }

    @Test
    public void shouldAppendCommandWithoutPayload() {
        //given
        final long commandTime = 9988776600001L;
        final int source = 1;
        final long seq = 22;
        final int type = 12345;

        //when
        when(timeSource.currentTime()).thenReturn(commandTime);
        senderSupplier.senderFor(source, seq).sendCommandWithoutPayload(type);

        //then
        assertCommand(source, seq, commandTime, type, null, commandStore.get(0));
    }

    private void assertCommand(final int source,
                               final long seq,
                               final long commandTime,
                               final int type,
                               final String text,
                               final Command command) {
        final int payloadSize = text == null ? 0 : Integer.BYTES + text.length();
        assertEquals(source, command.id().source(), "command.id.source");
        assertEquals(seq, command.id().sequence(), "command.id.sequence");
        assertEquals(commandTime, command.time(), "command.time");
        assertFalse(command.isAdmin(), "command.isAdmin");
        assertTrue(command.isApplication(), "command.isApplication");
        assertEquals(type, command.type(), "command.type");
        final DirectBuffer payload = command.payload();
        assertEquals(payloadSize, payload.capacity(), "command.payload.capacity");
        if (text != null) {
            assertEquals(text, payload.getStringAscii(0), "command.payload.text");
        }
    }

    private static DirectBuffer message(final String text, final int offset) {
        final int length = offset + Integer.BYTES + text.length();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(length);
        buffer.putStringAscii(offset, text);
        return buffer;
    }
}
