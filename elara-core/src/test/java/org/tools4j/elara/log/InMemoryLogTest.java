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
package org.tools4j.elara.log;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.log.MessageLog.AppendingContext;
import org.tools4j.elara.log.MessageLog.Handler;
import org.tools4j.elara.log.MessageLog.Poller;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.tools4j.elara.log.MessageLog.Handler.Result.POLL;

/**
 * Unit test for {@link InMemoryLog}
 */
class InMemoryLogTest {

    private static final int INITIAL_QUEUE_CAPACITY     = 4;
    private static final int INITIAL_BUFFER_CAPACITY    = 16;

    @Test
    public void appendAndPoll_withRemove() {
        //given
        final InMemoryLog messageLog = removeOnPollLog(false);

        //when + then
        final DirectBuffer[] messages = append(messageLog);
        poll(messageLog.poller(), messages);
        poll(messageLog.poller());
    }

    @Test
    public void appendAndPoll_withoutRemove() {
        //given
        final InMemoryLog messageLog = keepOnPollLog(true);

        //when + then
        final DirectBuffer[] messages = append(messageLog);
        poll(messageLog.poller(), messages);
        poll(messageLog.poller(), messages);
    }

    @Test
    public void appending() {
        //given
        final InMemoryLog messageLog = removeOnPollLog(true);

        //when + then
        final DirectBuffer[] messages = appending(messageLog);
        poll(messageLog.poller(), messages);
        poll(messageLog.poller());
    }

    @Test
    public void appendingWithAbort() {
        //given
        final InMemoryLog messageLog = removeOnPollLog(false);

        //when + then
        final DirectBuffer[] messages = appendingWithAbort(messageLog);
        poll(messageLog.poller(), messages);
        poll(messageLog.poller());
    }

    private DirectBuffer[] append(final InMemoryLog messageLog) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[]{
                message("Hi!"),
                message("Hello world!"),
                message("A somewhat longer message"),
                message("Peter and Paul"),
                message("a^2 + b^2 = c^2"),
        };
        final Appender appender = messageLog.appender();

        //when
        for (final DirectBuffer message : messages) {
            appender.append(message, 0, message.capacity());
        }

        //then
        assertEquals(messages.length, messageLog.size(), "size");
        return messages;
    }

    private DirectBuffer[] appending(final InMemoryLog messageLog) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[]{
                message("Hi!"),
                message("Hello world!"),
                message("A somewhat longer message"),
                message("Peter and Paul"),
                message("a^2 + b^2 = c^2"),
        };
        final Appender appender = messageLog.appender();

        //when
        for (final DirectBuffer message : messages) {
            try (final AppendingContext context = appender.appending()) {
                context.buffer().putBytes(0, message, 0, message.capacity());
                context.commit(message.capacity());
            }
        }

        //then
        assertEquals(messages.length, messageLog.size(), "size");
        return messages;
    }

    private DirectBuffer[] appendingWithAbort(final InMemoryLog messageLog) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[]{
                message("Hi!"),
                message("Hello world!"),
                message("A somewhat longer message"),
                message("Peter and Paul"),
                message("a^2 + b^2 = c^2"),
        };
        final List<DirectBuffer> committed = new ArrayList<>();
        final Appender appender = messageLog.appender();

        //when
        for (int i = 0; i < messages.length; i++) {
            final DirectBuffer message = messages[i];
            try (final AppendingContext context = appender.appending()) {
                context.buffer().putBytes(0, message, 0, message.capacity());
                if (i % 2 == 0) {
                    context.commit(message.capacity());
                    committed.add(message);
                } //else abort
            }
        }

        //then
        assertEquals(committed.size(), messageLog.size(), "size");
        return committed.toArray(new DirectBuffer[0]);
    }

    private void poll(final Poller poller,
                      final DirectBuffer... messages) {
        //given
        final MessageCaptor messageCaptor = new MessageCaptor();

        //when
        for (int i = 0; i < messages.length; i++) {
            final int p = poller.poll(messageCaptor.reset());

            //then
            assertEquals(1, p, "polled");
            assertEquals(0, messages[i].compareTo(messageCaptor.get()), "messages[" + i + "] compared");
        }

        //when
        final int p = poller.poll(messageCaptor.reset());

        //then
        assertEquals(0, p, "polled");
        assertNull(messageCaptor.get(), "polled message");
    }

    private static InMemoryLog keepOnPollLog(final boolean initEagerly) {
        return new InMemoryLog(INITIAL_QUEUE_CAPACITY, INITIAL_BUFFER_CAPACITY, false, initEagerly);
    }

    private static InMemoryLog removeOnPollLog(final boolean initEagerly) {
        return new InMemoryLog(INITIAL_QUEUE_CAPACITY, INITIAL_BUFFER_CAPACITY, true, initEagerly);
    }


    private static DirectBuffer message(final String msg) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(msg.length());
        buffer.putStringWithoutLengthAscii(0, msg);
        return buffer;
    }

    private static class MessageCaptor implements Handler {
        private DirectBuffer buffer;

        MessageCaptor reset() {
            buffer = null;
            return this;
        }

        DirectBuffer get() {
            return buffer;
        }

        @Override
        public Result onMessage(final DirectBuffer message) {
            buffer = new UnsafeBuffer(message, 0, message.capacity());
            return POLL;
        }
    }
}