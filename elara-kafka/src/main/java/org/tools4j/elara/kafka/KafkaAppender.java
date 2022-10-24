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
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.tools4j.elara.store.BufferingAppender;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.kafka.KafkaPoller.KEY_DESERIALIZER;
import static org.tools4j.elara.kafka.KafkaPoller.VALUE_DESERIALIZER;

public class KafkaAppender extends BufferingAppender {

    private static final LongSerializer KEY_SERIALIZER = new LongSerializer();
    private static final ByteArraySerializer VALUE_SERIALIZER = new ByteArraySerializer();
    private static final Duration SEEK_TO_END_WAIT = Duration.ofMillis(200);//FIXME configurable

    private Producer<Long, byte[]> producer;
    private final String topic;
    private final AtomicLong keyGenerator;

    public KafkaAppender(final Map<String, Object> config,
                         final String topic,
                         final int initialBufferCapacity) {
        this(config, topic, new ExpandableDirectByteBuffer(initialBufferCapacity));
    }

    public KafkaAppender(final Map<String, Object> config,
                         final String topic,
                         final MutableDirectBuffer buffer) {
        super(buffer);
        this.producer = new KafkaProducer<>(config, KEY_SERIALIZER, VALUE_SERIALIZER);
        this.topic = requireNonNull(topic);
        this.keyGenerator = new AtomicLong(keyOfLastRecord(appenderConsumerConfig(config), topic));
    }

    private static Map<String, Object> appenderConsumerConfig(final Map<String, Object> config) {
        final Object groupId = config.get(ConsumerConfig.GROUP_ID_CONFIG);
        final String appenderGroupId = groupId == null ? "elara-appender" : groupId + "-appender";
        final Map<String, Object> appenderConfig = new LinkedHashMap<>(config);
        appenderConfig.put(ConsumerConfig.GROUP_ID_CONFIG, appenderGroupId);
        return appenderConfig;
    }

    private static long keyOfLastRecord(final Map<String, Object> config, final String topic) {
        final KafkaConsumer<Long, byte[]> consumer = new KafkaConsumer<>(config, KEY_DESERIALIZER, VALUE_DESERIALIZER);
        try {
            consumer.subscribe(Collections.singletonList(topic));
            consumer.seekToEnd(Collections.emptyList());
            final ConsumerRecords<Long, ?> records = consumer.poll(SEEK_TO_END_WAIT);
            final AtomicLong max = new AtomicLong(-1);
            records.forEach(record -> max.accumulateAndGet(record.key(), Long::max));
            return max.get();
        } finally {
            consumer.close();
        }
    }

    @Override
    public void append(final DirectBuffer buffer, final int offset, final int length) {
        final byte[] bytes = new byte[length];
        buffer.getBytes(offset, bytes, 0, length);
        final long key = keyGenerator.incrementAndGet();
        unclosedProducer().send(new ProducerRecord<>(topic, key, bytes));
    }

    private Producer<Long, byte[]> unclosedProducer() {
        final Producer<Long, byte[]> producer = this.producer;
        if (producer != null) {
            return producer;
        }
        throw new IllegalStateException("Appender is closed");
    }

    @Override
    public boolean isClosed() {
        return producer == null;
    }

    @Override
    public void close() {
        if (producer != null) {
            producer.close();
            producer = null;
        }
    }
}
