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
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.Deserializer;
import org.tools4j.elara.stream.MessageReceiver;

import java.time.Duration;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.kafka.Codec.directBufferDeserializer;

public class KafkaReceiver implements MessageReceiver {
    private final RecordReceiver<?, ?> recordReceiver;

    public <K, V> KafkaReceiver(final Consumer<?, byte[]> consumer) {
        this(consumer, directBufferDeserializer());
    }
    public <K, V> KafkaReceiver(final Consumer<?, byte[]> consumer,
                                final Deserializer<? extends DirectBuffer> valueDeserializer) {
        this(consumer, recordTranslator(valueDeserializer));
    }

    public <K, V> KafkaReceiver(final Consumer<K, V> consumer,
                                final Function<? super ConsumerRecord<K, V>, ? extends DirectBuffer> recordTranslator) {
        this.recordReceiver = new RecordReceiver<>(consumer, recordTranslator, Duration.ZERO);
    }

    private static Function<? super ConsumerRecord<?, byte[]>, ? extends DirectBuffer> recordTranslator(
            final Deserializer<? extends DirectBuffer> valueDeserializer) {
        requireNonNull(valueDeserializer);
        return record -> valueDeserializer.deserialize(record.topic(), record.headers(), record.value());
    }

    @Override
    public int poll(final Handler handler) {
        return recordReceiver.poll(handler);
    }

    @Override
    public boolean isClosed() {
        return recordReceiver.consumer == null;
    }

    @Override
    public void close() {
        if (recordReceiver.consumer != null) {
            recordReceiver.consumer.close();
            recordReceiver.consumer = null;
        }
    }

    @Override
    public String toString() {
        return "KafkaReceiver";
    }

    private static class RecordReceiver<K, V> implements java.util.function.Consumer<ConsumerRecord<K, V>> {
        Consumer<K, V> consumer;
        Handler handler;
        final Function<? super ConsumerRecord<K, V>, ? extends DirectBuffer> recordTranslator;
        final Duration pollDuration;

        RecordReceiver(final Consumer<K, V> consumer,
                       final Function<? super ConsumerRecord<K, V>, ? extends DirectBuffer> recordTranslator,
                       final Duration pollDuration) {
            this.consumer = requireNonNull(consumer);
            this.recordTranslator = requireNonNull(recordTranslator);
            this.pollDuration = requireNonNull(pollDuration);
        }

        @Override
        public void accept(final ConsumerRecord<K, V> record) {
            handler.onMessage(recordTranslator.apply(record));
        }

        int poll(final Handler handler) {
            if (this.handler != handler) {
                this.handler = requireNonNull(handler);
            }
            if (consumer != null) {
                final ConsumerRecords<K, V> records = consumer.poll(pollDuration);
                records.forEach(this);
                consumer.commitAsync();
                return records.count();
            }
            return 0;
        }
    }
}
