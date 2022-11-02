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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.kafka.Codec.BYTE_ARRAY_DESERIALIZER;
import static org.tools4j.elara.kafka.Codec.DIRECT_BUFFER_SERIALIZER;
import static org.tools4j.elara.kafka.Codec.cast;
import static org.tools4j.elara.kafka.Codec.directBufferDeserializer;

public class Kafka {

    private static final int DEFAULT_INITIAL_SENDER_BUFFER_CAPACITY = 4096;
    private final Map<String, Object> config;
    private final int initialSenderBufferCapacity;

    public Kafka(final Map<String, Object> config) {
        this(config, DEFAULT_INITIAL_SENDER_BUFFER_CAPACITY);
    }
    public Kafka(final Map<String, Object> config, final int initialSenderBufferCapacity) {
        this.config = requireNonNull(config);
        this.initialSenderBufferCapacity = initialSenderBufferCapacity;
        if (initialSenderBufferCapacity < 0) {
            throw new IllegalArgumentException("Initial sender buffer capacity cannot be negative: " + initialSenderBufferCapacity);
        }
    }

    public KafkaSender sender(final String topic, final Serializer<? super DirectBuffer> keySerializer) {
        return sender(topic, keySerializer, DIRECT_BUFFER_SERIALIZER);
    }

    public KafkaSender sender(final String topic,
                              final Serializer<? super DirectBuffer> keySerializer,
                              final Serializer<? super DirectBuffer> valueSerializer) {
        return new KafkaSender(new KafkaProducer<>(config, cast(keySerializer), cast(valueSerializer)), topic, initialSenderBufferCapacity);
    }

    public <K, V> KafkaSender sender(final Producer<K, V> producer,
                                     final Function<? super DirectBuffer, ? extends ProducerRecord<K, V>> recordFactory) {
        return new KafkaSender(producer, recordFactory, initialSenderBufferCapacity);
    }

    public KafkaReceiver receiver(final String topic) {
        return receiver(topic, directBufferDeserializer());
    }

    public KafkaReceiver receiver(final String topic, final Deserializer<? extends DirectBuffer> valueDeserializer) {
        final KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(config, BYTE_ARRAY_DESERIALIZER, BYTE_ARRAY_DESERIALIZER);
        consumer.subscribe(Collections.singletonList(topic));
        return new KafkaReceiver(consumer, valueDeserializer);
    }

    public <K, V> KafkaReceiver receiver(final Consumer<K, V> consumer,
                                         final Function<? super ConsumerRecord<K, V>, ? extends DirectBuffer> recordTranslator) {
        return new KafkaReceiver(consumer, recordTranslator);
    }
}
