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
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.BrokerNotAvailableException;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.stream.MessageSender;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class KafkaSender extends MessageSender.Buffered {

    private RecordProducer<?> recordProducer;
    private final DirectBuffer view = new UnsafeBuffer(0, 0);

    public <K> KafkaSender(final Producer<K, byte[]> producer,
                           final String topic,
                           final int initialBufferCapacity) {
        this(producer, buffer -> new ProducerRecord<>(topic, toByteArray(buffer)), initialBufferCapacity);
    }

    public <K> KafkaSender(final Producer<K, byte[]> producer,
                           final String topic,
                           final Function<? super DirectBuffer, ? extends K> keyExtractor,
                           final int initialBufferCapacity) {
        this(producer, buffer -> new ProducerRecord<>(topic, keyExtractor.apply(buffer), toByteArray(buffer)), initialBufferCapacity);
    }

    public <K> KafkaSender(final Producer<K, byte[]> producer,
                           final Function<? super DirectBuffer, ? extends ProducerRecord<K, byte[]>> recordFactory,
                           final int initialBufferCapacity) {
        super(initialBufferCapacity);
        this.recordProducer = new RecordProducer<>(producer, recordFactory);
    }

    static byte[] toByteArray(final DirectBuffer buffer) {
        final byte[] bytes = new byte[buffer.capacity()];
        buffer.getBytes(0, bytes);
        return bytes;
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        if (recordProducer != null) {
            view.wrap(buffer, offset, length);
            return recordProducer.send(view);
        }
        return SendingResult.CLOSED;
    }

    @Override
    public boolean isClosed() {
        return recordProducer != null;
    }

    @Override
    public void close() {
        if (recordProducer != null) {
            recordProducer.producer.close();
            recordProducer = null;
        }
    }

    private static class RecordProducer<K> {
        final Producer<K, byte[]> producer;
        final Function<? super DirectBuffer, ? extends ProducerRecord<K, byte[]>> recordFactory;

        RecordProducer(final Producer<K, byte[]> producer,
                       final Function<? super DirectBuffer, ? extends ProducerRecord<K, byte[]>> recordFactory) {
            this.producer = requireNonNull(producer);
            this.recordFactory = requireNonNull(recordFactory);
        }

        SendingResult send(final DirectBuffer message) {
            try {
                final ProducerRecord<K, byte[]> record = recordFactory.apply(message);
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
