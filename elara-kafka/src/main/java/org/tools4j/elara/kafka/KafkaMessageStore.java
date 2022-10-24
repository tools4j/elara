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

import org.agrona.CloseHelper;
import org.tools4j.elara.store.MessageStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static java.util.Objects.requireNonNull;

public class KafkaMessageStore implements MessageStore {

    private final Map<String, Object> configs;
    private final String topic;
    private final UnaryOperator<Appender> appenderInitialiser;
    private final AtomicReference<Appender> appender = new AtomicReference<>();
    private final List<Poller> pollers = new ArrayList<>();

    public KafkaMessageStore(final Map<String, Object> configs, final String topic, final int initialSenderBufferCapacity) {
        this.configs = requireNonNull(configs);
        this.topic = requireNonNull(topic);
        this.appenderInitialiser = appender -> {
            if (appender == null) {
                return new KafkaAppender(configs, topic, initialSenderBufferCapacity);
            }
            return appender;
        };
    }

    @Override
    public Appender appender() {
        ensoreNotClosed();
        return appender.updateAndGet(appenderInitialiser);
    }

    @Override
    public Poller poller() {
        ensoreNotClosed();
        final Poller poller = new KafkaPoller(configs, topic, "message-store-default-poller-" + pollers.size());
        pollers.add(poller);
        return poller;
    }

    @Override
    public Poller poller(final String id) {
        ensoreNotClosed();
        final Poller poller = new KafkaPoller(configs, topic, id);
        pollers.add(poller);
        return poller;
    }

    private void ensoreNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("Kafka message store is closed");
        }
    }

    @Override
    public boolean isClosed() {
        final Appender appender = this.appender.get();
        return appender != null && appender.isClosed();
    }

    @Override
    public void close() {
        if (!appender.compareAndSet(null, Appender.CLOSED)) {
            appender.get().close();
        }
        closePollers();
    }

    private void closePollers() {
        CloseHelper.quietCloseAll(pollers);
        pollers.clear();
    }
}
