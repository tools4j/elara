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
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.tools4j.elara.stream.MessageReceiver;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class KafkaReceiver implements MessageReceiver {

    private Consumer<?, byte[]> consumer;
    private Handler handler;
    private final DirectBuffer view = new UnsafeBuffer(0, 0);
    private final java.util.function.Consumer<ConsumerRecord<?, byte[]>> messageHandler = this::onMessage;

    public KafkaReceiver(final Consumer<?, byte[]> consumer) {
        this.consumer = requireNonNull(consumer);
    }

    @Override
    public int poll(final Handler handler) {
        if (this.handler != handler) {
            this.handler = requireNonNull(handler);
        }
        if (consumer != null) {
            final ConsumerRecords<?, byte[]> records = consumer.poll(Duration.ZERO);
            records.forEach(messageHandler);
            consumer.commitAsync();
            return records.count();
        }
        return 0;
    }

    private void onMessage(final ConsumerRecord<?, byte[]> record) {
        view.wrap(record.value());
        try {
            handler.onMessage(view);
        } finally {
            view.wrap(0, 0);
        }
    }

    @Override
    public boolean isClosed() {
        return consumer == null;
    }

    @Override
    public void close() {
        if (consumer != null) {
            consumer.close();
            consumer = null;
        }
    }

    @Override
    public String toString() {
        return "KafkaReceiver";
    }
}
