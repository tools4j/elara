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
package org.tools4j.elara.kafka;

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.BrokerNotAvailableException;
import org.apache.kafka.common.serialization.Serializer;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class KafkaSender extends MessageSender.Buffered {

    private RecordSender<?, ?> recordSender;
    private final DirectBuffer view = new UnsafeBuffer(0, 0);

    public KafkaSender(final Producer<byte[], byte[]> producer,
                       final String topic,
                       final Serializer<? super DirectBuffer> keySerializer,
                       final int initialBufferCapacity) {
        this(producer, topic, keySerializer, Codec.DIRECT_BUFFER_SERIALIZER, initialBufferCapacity);
    }

    public KafkaSender(final Producer<byte[], byte[]> producer,
                       final String topic,
                       final Serializer<? super DirectBuffer> keySerializer,
                       final Serializer<? super DirectBuffer> valueSerializer,
                       final int initialBufferCapacity) {
        this(producer, recordFactory(topic, keySerializer, valueSerializer), initialBufferCapacity);
    }

    public KafkaSender(final Producer<DirectBuffer, DirectBuffer> producer,
                       final String topic,
                       final int initialBufferCapacity) {
        this(producer, recordFactory(topic), initialBufferCapacity);
    }

    public <K, V> KafkaSender(final Producer<K, V> producer,
                              final Function<? super DirectBuffer, ? extends ProducerRecord<K, V>> recordFactory,
                              final int initialBufferCapacity) {
        super(initialBufferCapacity);
        this.recordSender = new RecordSender<>(producer, recordFactory);
    }

    private static Function<? super DirectBuffer, ? extends ProducerRecord<DirectBuffer, DirectBuffer>> recordFactory(
            final String topic) {
        requireNonNull(topic);
        return buffer -> new ProducerRecord<>(topic, buffer, buffer);
    }

    private static Function<? super DirectBuffer, ? extends ProducerRecord<byte[], byte[]>> recordFactory(
            final String topic,
            final Serializer<? super DirectBuffer> keySerializer,
            final Serializer<? super DirectBuffer> valueSerializer) {
        requireNonNull(topic);
        requireNonNull(keySerializer);
        requireNonNull(valueSerializer);
        return buffer -> new ProducerRecord<>(topic, keySerializer.serialize(topic, buffer), valueSerializer.serialize(topic, buffer));
    }

    /**
     * Returns the underlying Kafka producer, or null if this sender has already been closed.
     * @return underlying Kafka producer, or null if closed.
     */
    public Producer<?, ?> producer() {
        return recordSender.producer;
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        if (recordSender != null) {
            view.wrap(buffer, offset, length);
            return recordSender.send(view);
        }
        return SendingResult.CLOSED;
    }

    @Override
    public boolean isClosed() {
        return recordSender != null;
    }

    @Override
    public void close() {
        if (recordSender != null) {
            recordSender.producer.close();
            recordSender = null;
        }
    }

    private static class RecordSender<K, V> {
        final Producer<K, V> producer;
        final Function<? super DirectBuffer, ? extends ProducerRecord<K, V>> recordFactory;

        RecordSender(final Producer<K, V> producer,
                     final Function<? super DirectBuffer, ? extends ProducerRecord<K, V>> recordFactory) {
            this.producer = requireNonNull(producer);
            this.recordFactory = requireNonNull(recordFactory);
        }

        SendingResult send(final DirectBuffer message) {
            try {
                final ProducerRecord<K, V> record = recordFactory.apply(message);
                producer.send(record);
                return SendingResult.SENT;
            } catch (final BrokerNotAvailableException e) {
                return SendingResult.DISCONNECTED;
            } catch (final Exception e) {
                //FIXME log
                return SendingResult.FAILED;
            }
        }
    }

    @Override
    public String toString() {
        return "KafkaSender";
    }
}
