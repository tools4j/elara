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
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.input.DefaultReceiver;
import org.tools4j.elara.stream.MessageStream.Appender;
import org.tools4j.elara.stream.MessageStream.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultReceiver}
 */
@ExtendWith(MockitoExtension.class)
public class DefaultReceiverTest {

    @Mock
    private TimeSource timeSource;

    private List<Command> commandStore;

    //under test
    private DefaultReceiver defaultReceiver;

    @BeforeEach
    public void init() {
        commandStore = new ArrayList<>();
        defaultReceiver = new DefaultReceiver(timeSource, new Appender() {
            @Override
            public AppendingContext appending() {
                return new AppendingContext() {
                    MutableDirectBuffer buffer = new ExpandableArrayBuffer();
                    @Override
                    public MutableDirectBuffer buffer() {
                        return buffer;
                    }

                    @Override
                    public void abort() {
                        buffer = null;
                    }

                    @Override
                    public void commit(final int length) {
                        if (buffer != null) {
                            commandStore.add(new FlyweightCommand().init(buffer, 0));
                            buffer = null;
                        }
                    }

                    @Override
                    public boolean isClosed() {
                        return buffer == null;
                    }
                };
            }

            @Override
            public void close() {
                //no op
            }
        });
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
        defaultReceiver.receiveMessage(source, seq, message, offset, length);

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
        defaultReceiver.receiveMessage(source, seq, type, message, offset, length);

        //then
        assertCommand(source, seq, commandTime, type, text, commandStore.get(0));
    }

    private void assertCommand(final int source,
                               final long seq,
                               final long commandTime,
                               final int tpye,
                               final String text,
                               final Command command) {
        final int payloadSize = Integer.BYTES + text.length();
        assertEquals(source, command.id().source(), "command.id.source");
        assertEquals(seq, command.id().sequence(), "command.id.sequence");
        assertEquals(commandTime, command.time(), "command.time");
        assertFalse(command.isAdmin(), "command.isAdmin");
        assertTrue(command.isApplication(), "command.isApplication");
        assertEquals(tpye, command.type(), "command.type");
        final DirectBuffer payload = command.payload();
        assertEquals(payloadSize, payload.capacity(), "command.payload.capacity");
        assertEquals(text, payload.getStringAscii(0), "command.payload.text");
    }

    private static DirectBuffer message(final String text, final int offset) {
        final int length = offset + Integer.BYTES + text.length();
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(length);
        buffer.putStringAscii(offset, text);
        return buffer;
    }
}
