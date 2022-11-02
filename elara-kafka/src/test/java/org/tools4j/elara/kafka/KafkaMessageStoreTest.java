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
package org.tools4j.elara.kafka;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.streams.integration.utils.EmbeddedKafkaCluster;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Poller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.kafka.KafkaMessageStreamTest.assumeNotWindows;
import static org.tools4j.elara.store.MessageStore.Handler.Result.POLL;

/**
 * Unit test for {@link KafkaMessageStore}.
 */
class KafkaMessageStoreTest {

    private static final String TOPIC = "elara-store";

    private static EmbeddedKafkaCluster cluster;

    @BeforeAll
    static void startKafkaCluster() throws IOException {
        assumeNotWindows();
        cluster = new EmbeddedKafkaCluster(1);
        cluster.start();
    }

    @AfterAll
    static void stopKafkaCluster() {
        if (cluster != null) {
            try {
                cluster.stop();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            cluster = null;
        }
    }

    @BeforeEach
    void createTopic() throws InterruptedException {
//        final long start = System.currentTimeMillis();
        cluster.deleteAllTopicsAndWait(30_000);
        cluster.createTopic(TOPIC);
//        System.out.println("topic delete/create time: " + (System.currentTimeMillis() - start)/1000f + "s");
    }

    @Test
    public void appendAndPoll() {
        //given
        final KafkaMessageStore messageStore = kafkaMessageStore();

        //when + then
        final DirectBuffer[] messages = append(messageStore);
        pollAndAssert(messageStore.poller(), messages);
        pollAndAssert(messageStore.poller());
        pollAndAssert(messageStore.poller("remember-position"), messages);
        pollAndAssert(messageStore.poller("remember-position"));
    }

    @Test
    public void appending() {
        //given
        final MessageStore messageStore = kafkaMessageStore();

        //when + then
        final DirectBuffer[] messages = appending(messageStore);
        pollAndAssert(messageStore.poller(), messages);
        pollAndAssert(messageStore.poller());
    }


    @Test
    public void appendingWithAbort() {
        //given
        final MessageStore messageStore = kafkaMessageStore();

        //when + then
        appendingWithAbort(messageStore);
        pollAndAssert(messageStore.poller());
    }

    @Test
    public void moveTo() {
        //given
        final MessageStore messageStore = kafkaMessageStore();
        final DirectBuffer[] messages = append(messageStore);
        final Poller poller = messageStore.poller();
        final long firstEntryId = poller.entryId();
        final long secondEntryId;
        final long lastEntryId;
        final long afterLastEntryId;

        //when + then
        assertEquals(0, poller.entryId(), "[0]poller.entryId");
        assertFalse(poller.moveToPrevious(), "[0]poller.moveToPrevious");
        assertTrue(poller.moveToNext(), "[0]poller.moveToNext");
        assertEquals(1, poller.entryId(), "[1]poller.entryId");
        secondEntryId = poller.entryId();
        assertTrue(poller.moveToNext(), "[1]poller.moveToNext");
        assertEquals(2, poller.entryId(), "[2]poller.entryId");

        //when
        poller.moveToEnd();
        afterLastEntryId = poller.entryId();

        //then
        assertEquals(messages.length, poller.entryId(), "[" + messages.length + "]poller.entryId");

        //when + then
        assertTrue(poller.moveToPrevious(), "[" + messages.length + "]poller.moveToPrevious");
        assertEquals(messages.length - 1, poller.entryId(), "[" + (messages.length - 1) + "]poller.entryId");
        lastEntryId = poller.entryId();

        //when
        poller.moveToStart();

        //then
        assertEquals(0, poller.entryId(), "[0]poller.entryId");

        //when + then
        for (int i = 0; i < messages.length; i++) {
            assertTrue(poller.moveToNext(), "[" + i + "]poller.moveToNext");
            assertEquals(i + 1, poller.entryId(), "[" + (i+1) + "]poller.entryId");
        }
        assertFalse(poller.moveToNext(), "[" + messages.length + "]poller.moveToNext");
        for (int i = messages.length - 1; i >= 0; i--) {
            assertTrue(poller.moveToPrevious(), "[" + i + "]poller.moveToPrevious");
            assertEquals(i, poller.entryId(), "[" + i + "]poller.entryId");
        }
        assertFalse(poller.moveToPrevious(), "[0]poller.moveToPrevious");
        assertEquals(0, poller.entryId(), "[0]poller.entryId");

        //when + then
        assertFalse(poller.moveTo(afterLastEntryId), "poller.moveTo(afterLastEntryId)");
        assertEquals(0, poller.entryId(), "[0]poller.entryId");
        assertTrue(poller.moveTo(lastEntryId), "poller.moveTo(lastEntryId)");
        assertEquals(messages.length - 1, poller.entryId(), "[lastEntryId]poller.entryId");
        assertTrue(poller.moveTo(firstEntryId), "poller.moveTo(firstEntryId)");
        assertEquals(0, poller.entryId(), "[firstEntryId]poller.entryId");
        assertTrue(poller.moveTo(secondEntryId), "poller.moveTo(secondEntryId)");
        assertEquals(1, poller.entryId(), "[secondEntryId]poller.entryId");
    }

    @ValueSource(ints = {0, 1, 2, 3, 4})
    @ParameterizedTest(name = "moveToAndPoll({0})")
    void moveToAndPoll(final int position) {
        //given
        final MessageStore messageStore = kafkaMessageStore();
        final DirectBuffer[] messages = append(messageStore);
        final Poller poller = messageStore.poller();

        //when + then
        assertTrue(poller.moveTo(position), "poller.moveTo");
        assertEquals(position, poller.entryId(), "poller.entryId");
        pollAndAssert(poller, Arrays.copyOfRange(messages, position, messages.length));
    }

    @Test
    void moveToEndAndPoll() {
        //given
        final MessageStore messageStore = kafkaMessageStore();
        final DirectBuffer[] messages = append(messageStore);
        final Poller poller = messageStore.poller();

        //when + then
        poller.moveToEnd();
        assertEquals(messages.length, poller.entryId(), "poller.entryId");
        pollAndAssert(poller);
        poller.moveToPrevious();
        poller.moveToPrevious();
        poller.moveToEnd();
        assertEquals(messages.length, poller.entryId(), "poller.entryId");
        pollAndAssert(poller);
    }

    @Test
    void moveToLastAndPoll() {
        //given
        final MessageStore messageStore = kafkaMessageStore();
        final DirectBuffer[] messages = append(messageStore);
        final Poller poller = messageStore.poller();

        //when + then
        poller.moveToEnd();
        poller.moveToPrevious();
        assertEquals(messages.length - 1, poller.entryId(), "poller.entryId");
        pollAndAssert(poller, messages[messages.length - 1]);
        poller.moveToPrevious();
        assertEquals(messages.length - 1, poller.entryId(), "poller.entryId");
        pollAndAssert(poller, messages[messages.length - 1]);
    }

    @ValueSource(ints = {0, 1, 2, 3, 4})
    @ParameterizedTest(name = "pollMoveToPoll({0})")
    void pollMoveToPoll(final int position) {
        //given
        final MessageStore messageStore = kafkaMessageStore();
        final DirectBuffer[] messages = append(messageStore);
        final Poller poller = messageStore.poller();

        //when + then
        pollAndAssert(poller, messages);
        assertTrue(poller.moveTo(position), "poller.moveTo");
        assertEquals(position, poller.entryId(), "poller.entryId");
        pollAndAssert(poller, Arrays.copyOfRange(messages, position, messages.length));
    }

    @Test
    void pollMoveToStartPoll() {
        //given
        final MessageStore messageStore = kafkaMessageStore();
        final DirectBuffer[] messages = append(messageStore);
        final Poller poller = messageStore.poller();

        //when + then
        pollAndAssert(poller, messages);
        poller.moveToStart();
        assertEquals(0, poller.entryId(), "poller.entryId");
        pollAndAssert(poller, messages);
    }

    private DirectBuffer[] append(final MessageStore messageStore) {
        //given
        final DirectBuffer[] messages = new DirectBuffer[] {
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

    private void pollAndAssert(final Poller poller, final DirectBuffer... messages) {
        poll(poller, true, messages);
    }

    private void poll(final Poller poller,
                      final boolean assertMessage,
                      final DirectBuffer... messages) {
        //given
        final MessageCaptor messageCaptor = new MessageCaptor();

        //when
        final int maxPolls = 1_000_000;
        int received;
        for (received = 0; received < messages.length; ) {
            int p = 0;
            messageCaptor.reset();
            for (int i = 0; i < maxPolls && p == 0; i++) {
                p = poller.poll(messageCaptor);
            }

            //then
            assertTrue(p > 0, "polled[" + received + "]");
            assertEquals(p, messageCaptor.count(), "polled[" + received + "]");
            if (assertMessage) {
                for (int i = 0; i < p; i++) {
                    assertEquals(0, messages[received + i].compareTo(messageCaptor.get(i)),
                            "messages[" + (received + i) + "] compared");
                }
            }
            received += p;
        }
        assertEquals(messages.length, received, "received");

        //when
        final int p = poller.poll(messageCaptor.reset());

        //then
        assertEquals(0, p, "polled");
        assertEquals(0, messageCaptor.count(), "polled message count");
    }

    private static DirectBuffer message(final String msg) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(msg.length());
        buffer.putStringWithoutLengthAscii(0, msg);
        return buffer;
    }

    private static class MessageCaptor implements Handler {
        final List<DirectBuffer> captured = new ArrayList<>();

        MessageCaptor reset() {
            captured.clear();
            return this;
        }

        int count() {
            return captured.size();
        }

        DirectBuffer get(final int i) {
            return captured.get(i);
        }

        @Override
        public Result onMessage(final DirectBuffer message) {
            final MutableDirectBuffer copy = new ExpandableArrayBuffer(message.capacity());
            copy.putBytes(0, message, 0, message.capacity());
            captured.add(copy);
            return POLL;
        }
    }

    private KafkaMessageStore kafkaMessageStore() {
        return new KafkaMessageStore(kafkaConfig(), TOPIC, 4096);
    }

    private static Map<String, Object> kafkaConfig() {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, cluster.bootstrapServers());
        config.put(ProducerConfig.RETRIES_CONFIG, 0);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-message-store-test-consumer");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        return config;
    }
}