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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Poller;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.store.MessageStore.Handler.Result.POLL;

/**
 * Unit test for {@link MessageStore}.
 */
class MessageStoreTest {

    @Test
    public void appendAndPoll(final TestInfo testInfo) {
        //given
        final MessageStore messageStore = chronicleMessageStore(testInfo);

        //when + then
        final DirectBuffer[] messages = append(messageStore);
        pollAndAssert(messageStore.poller(), messages);
        pollAndAssert(messageStore.poller(), messages);
        pollAndAssert(messageStore.poller("remember-position"), messages);
        pollAndAssert(messageStore.poller("remember-position"));
    }

    @Test
    public void appending(final TestInfo testInfo) {
        //given
        final MessageStore messageStore = chronicleMessageStore(testInfo);

        //when + then
        final DirectBuffer[] messages = appending(messageStore);
        pollAndAssert(messageStore.poller(), messages);
        pollAndAssert(messageStore.poller(), messages);
    }

    @Test
    public void appendingFromMultipleThreads(final TestInfo testInfo) throws InterruptedException {
        //given
        final int nThreads = 5;
        final MessageStore messageStore = chronicleMessageStore(testInfo);
        ConcurrentLinkedQueue<DirectBuffer> messageQueue = new ConcurrentLinkedQueue<>();

        //when
        final Thread[] appenders = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            final Runnable r = () -> {
                messageQueue.addAll(Arrays.asList(appending(messageStore)));
                sleep(20);//given other threads chance for interleaved appending
                messageQueue.addAll(Arrays.asList(appending(messageStore)));
            };
            appenders[i] = new Thread(null, r, "appender-" + i);
            appenders[i].start();
        }
        for (int i = 0; i < nThreads; i++) {
            appenders[i].join(5000);
        }

        //then
        final DirectBuffer[] messages = messageQueue.toArray(new DirectBuffer[0]);
        poll(messageStore.poller(), false, messages);
        poll(messageStore.poller(), false, messages);
    }

    @Test
    public void appendingWithAbort(final TestInfo testInfo) {
        //given
        final MessageStore messageStore = chronicleMessageStore(testInfo);

        //when + then
        final DirectBuffer[] messages = appendingWithAbort(messageStore);
        pollAndAssert(messageStore.poller(), messages);
    }

    @Test
    public void moveTo(final TestInfo testInfo) {
        //given
        final ChronicleMessageStore messageStore = chronicleMessageStore(testInfo);
        final DirectBuffer[] messages = append(messageStore);
        final ChroniclePoller poller = messageStore.poller();
        final long firstEntryId = poller.entryId();
        final long secondEntryId;
        final long lastEntryId;
        final long afterLastEntryId;

        //when + then
        assertEquals(0, poller.sequence(), "[0]poller.sequence");
        assertFalse(poller.moveToPrevious(), "[0]poller.moveToPrevious");
        assertTrue(poller.moveToNext(), "[0]poller.moveToNext");
        assertEquals(1, poller.sequence(), "[1]poller.sequence");
        secondEntryId = poller.entryId();
        assertTrue(poller.moveToNext(), "[1]poller.moveToNext");
        assertEquals(2, poller.sequence(), "[2]poller.sequence");

        //when
        poller.moveToEnd();
        afterLastEntryId = poller.entryId();

        //then
        assertEquals(messages.length, poller.sequence(), "[" + messages.length + "]poller.sequence");

        //when + then
        assertTrue(poller.moveToPrevious(), "[" + messages.length + "]poller.moveToPrevious");
        assertEquals(messages.length - 1, poller.sequence(), "[" + (messages.length - 1) + "]poller.sequence");
        lastEntryId = poller.entryId();

        //when
        poller.moveToStart();

        //then
        assertEquals(0, poller.sequence(), "[0]poller.sequence");

        //when + then
        for (int i = 0; i < messages.length; i++) {
            assertTrue(poller.moveToNext(), "[" + i + "]poller.moveToNext");
            assertEquals(i + 1, poller.sequence(), "[" + (i+1) + "]poller.sequence");
        }
        assertFalse(poller.moveToNext(), "[" + messages.length + "]poller.moveToNext");
        for (int i = messages.length - 1; i >= 0; i--) {
            assertTrue(poller.moveToPrevious(), "[" + i + "]poller.moveToPrevious");
            assertEquals(i, poller.sequence(), "[" + i + "]poller.sequence");
        }
        assertFalse(poller.moveToPrevious(), "[0]poller.moveToPrevious");
        assertEquals(0, poller.sequence(), "[0]poller.sequence");

        //when + then
        assertFalse(poller.moveTo(afterLastEntryId), "poller.moveTo(afterLastEntryId)");
        assertEquals(0, poller.sequence(), "[0]poller.sequence");
        assertTrue(poller.moveTo(lastEntryId), "poller.moveTo(lastEntryId)");
        assertEquals(messages.length - 1, poller.sequence(), "[lastEntryId]poller.sequence");
        assertTrue(poller.moveTo(firstEntryId), "poller.moveTo(firstEntryId)");
        assertEquals(0, poller.sequence(), "[firstEntryId]poller.sequence");
        assertTrue(poller.moveTo(secondEntryId), "poller.moveTo(secondEntryId)");
        assertEquals(1, poller.sequence(), "[secondEntryId]poller.sequence");
    }

    private DirectBuffer[] append(final MessageStore messageStore) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[]{
                message("Hi!"),
                message("Hello world!"),
                message("A somewhat longer message"),
                message("Peter and Paul"),
                message("a^2 + b^2 = c^2"),
        };
        final Appender appender = messageStore.appender();

        //when
        for (final DirectBuffer message : messages) {
            appender.append(message, 0, message.capacity());
        }

        //then
        return messages;
    }

    private DirectBuffer[] appending(final MessageStore messageStore) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[]{
                message("Hi!"),
                message("Hello world!"),
                message("A somewhat longer message"),
                message("Peter and Paul"),
                message("a^2 + b^2 = c^2"),
        };
        final Appender appender = messageStore.appender();

        //when
        for (final DirectBuffer message : messages) {
            try (final AppendingContext context = appender.appending()) {
                context.buffer().putBytes(0, message, 0, message.capacity());
                context.commit(message.capacity());
            }
        }

        //then
        return messages;
    }

    private DirectBuffer[] appendingWithAbort(final MessageStore messageStore) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[]{
                message("Hi!"),
                message("Hello world!"),
                message("A somewhat longer message"),
                message("Peter and Paul"),
                message("a^2 + b^2 = c^2"),
        };
        final List<DirectBuffer> committed = new ArrayList<>();
        final Appender appender = messageStore.appender();

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
        return committed.toArray(new DirectBuffer[0]);
    }

    private void pollAndAssert(final Poller poller,
                               final DirectBuffer... messages) {
        poll(poller, true, messages);
    }

    private void poll(final Poller poller,
                      final boolean assertMessage,
                      final DirectBuffer... messages) {
        //given
        final MessageCaptor messageCaptor = new MessageCaptor();

        //when
        for (int i = 0; i < messages.length; i++) {
            final int p = poller.poll(messageCaptor.reset());

            //then
            assertEquals(1, p, "polled");
            if (assertMessage) {
                assertEquals(0, messages[i].compareTo(messageCaptor.get()), "messages[" + i + "] compared");
            }
        }

        //when
        final int p = poller.poll(messageCaptor.reset());

        //then
        assertEquals(0, p, "polled");
        assertNull(messageCaptor.get(), "polled message");
    }

    private ChronicleMessageStore chronicleMessageStore(final TestInfo testInfo) {
        final String fileName = testInfo.getTestClass().get().getSimpleName() + "_" + testInfo.getTestMethod().get().getName();
        final String path = "build/chronicle/" + fileName + ".cq4";
        delete(path);
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path(path)
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return new ChronicleMessageStore(cq);
    }

    private static void delete(final String path) {
        final File dir = new File(path);
        final File[] files = dir.isDirectory() ? dir.listFiles() : null;
        for (final File file : files == null ? new File[0] : files) {
            file.delete();
        }
        dir.delete();
        if (dir.exists()) {
            throw new IllegalStateException("Exists: " + dir.getAbsolutePath());
        }
    }
    private static DirectBuffer message(final String msg) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(msg.length());
        buffer.putStringWithoutLengthAscii(0, msg);
        return buffer;
    }

    private static class MessageCaptor implements Handler {
        private MutableDirectBuffer buffer;

        MessageCaptor reset() {
            buffer = null;
            return this;
        }

        DirectBuffer get() {
            return buffer;
        }

        @Override
        public Result onMessage(final DirectBuffer message) {
            buffer = new ExpandableArrayBuffer(message.capacity());
            buffer.putBytes(0, message, 0, message.capacity());
            return POLL;
        }
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}