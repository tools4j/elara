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
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.kafka.KafkaSender.toByteArray;

public class Kafka {

    private static final ByteArraySerializer VALUE_SERIALIZER = new ByteArraySerializer();
    private static final ByteArrayDeserializer VALUE_DESERIALIZER = new ByteArrayDeserializer();

    private final Map<String, Object> config;

    public Kafka(final Map<String, Object> config) {
        this.config = requireNonNull(config);
    }

    <K> KafkaSender sender(final String topic,
                           final Function<? super DirectBuffer, ? extends K> keyExtractor,
                           final Serializer<K> keySerializer) {
        return sender(new KafkaProducer<>(config, keySerializer, VALUE_SERIALIZER),
                message -> new ProducerRecord<>(topic, keyExtractor.apply(message), toByteArray(message)));
    }

    <K> KafkaSender sender(final Producer<K, byte[]> producer,
                           final Function<? super DirectBuffer, ? extends ProducerRecord<K, byte[]>> recordFactory) {
        return new KafkaSender(producer, recordFactory, 4096);
    }

    <K> KafkaReceiver receiver(final String topic, final Deserializer<K> keyDeserializer) {
        final KafkaConsumer<K, byte[]> consumer = new KafkaConsumer<>(config, keyDeserializer, VALUE_DESERIALIZER);
        consumer.subscribe(Collections.singletonList(topic));
        return receiver(consumer);
    }

    <K> KafkaReceiver receiver(final Consumer<K, byte[]> kafkaConsumer) {
        return new KafkaReceiver(kafkaConsumer);
    }
}
