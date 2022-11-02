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
import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.UnsafeBuffer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.store.MessageStore.Poller;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

public class KafkaPoller implements Poller {

    private static final Duration MOVE_MAX_WAIT = Duration.ofSeconds(10);//FIXME configurable
    static final LongDeserializer KEY_DESERIALIZER = new LongDeserializer();
    static final ByteArrayDeserializer VALUE_DESERIALIZER = new ByteArrayDeserializer();

    private final String topic;
    private final DirectBuffer view = new UnsafeBuffer(0, 0);
    private final RecordStamp lastRecord = new RecordStamp();

    private KafkaConsumer<Long, byte[]> consumer;
    private Handler handler;
    private TopicPartition topicPartition;

    public KafkaPoller(final Map<String, Object> config, final String topic, final String groupId) {
        this.topic = requireNonNull(topic);
        this.consumer = new KafkaConsumer<>(configWithGroupId(config, groupId), KEY_DESERIALIZER, VALUE_DESERIALIZER);
        consumer.subscribe(Collections.singletonList(topic));
    }

    static Map<String, Object> configWithGroupId(final Map<String, Object> config, final String groupId) {
        final Map<String, Object> configWithGroupId = new LinkedHashMap<>(config);
        configWithGroupId.put(CommonClientConfigs.GROUP_ID_CONFIG, requireNonNull(groupId));
        return configWithGroupId;
    }

    private TopicPartition topicPartition(final int partition) {
        if (topicPartition == null || topicPartition.partition() != partition) {
            topicPartition = new TopicPartition(topic, partition);
        }
        return topicPartition;
    }


    @Override
    public int poll(final Handler handler) {
        if (this.handler != handler) {
            this.handler = requireNonNull(handler);
        }
        if (consumer != null) {
            final ConsumerRecords<?, byte[]> records = consumer.poll(Duration.ZERO);
            if (records != null && !records.isEmpty()) {
                int polled = 0;
                for (final ConsumerRecord<?, byte[]> record : records) {
                    final Handler.Result result = onMessage(record);
                    if (result == Result.POLL) {
                        lastRecord.init(record);
                        polled++;
                    } else {
                        break;
                    }
                }
                if (lastRecord.next()) {
                    consumer.commitAsync(lastRecord.offsetMap(), null);
                }
                if (polled < records.count()) {
                    consumer.seek(topicPartition, lastRecord.offset);
                }
                return Math.max(1, polled);//return at least 1 to indicate that some work was performed
            }
        }
        return 0;
    }

    private Result onMessage(final ConsumerRecord<?, byte[]> record) {
        view.wrap(record.value());
        try {
            return handler.onMessage(view);
        } finally {
            view.wrap(0, 0);
        }
    }

    @Override
    public long entryId() {
        return lastRecord.offset;
    }

    private long maxEndOffset() {
        if (consumer.assignment().isEmpty()) {
            consumer.poll(MOVE_MAX_WAIT);
        }
        final Map<TopicPartition, Long> endOffsets = consumer.endOffsets(consumer.assignment());
        return endOffsets.values().stream().mapToLong(Long::longValue).max().orElse(0);
    }

    @Override
    public boolean moveTo(final long entryId) {
        if (entryId == 0) {
            moveToStart();
            return true;
        }
        if (lastRecord.offset == entryId && entryId <= lastRecord.maxConsumedOffset) {
            return true;
        }
        if (entryId == lastRecord.offset + 1) {
            if (lastRecord.next()) {
                consumer.seek(lastRecord.topicPartition(), lastRecord.offset);
                return true;
            }
            //keep trying below
        } else if (entryId == lastRecord.offset - 1) {
            if (lastRecord.previous()) {
                consumer.seek(lastRecord.topicPartition(), lastRecord.offset);
                return true;
            }
            //no need to try below
            return false;
        }
        if (entryId < 0) {
            return false;
        }
        if (entryId > lastRecord.maxConsumedOffset) {
            final long maxEndOffset = maxEndOffset();
            lastRecord.maxConsumedOffset = Math.max(lastRecord.maxConsumedOffset, maxEndOffset - 1);
        }
        if (entryId <= lastRecord.maxConsumedOffset) {
            lastRecord.offset = entryId;
        }
        consumer.seek(lastRecord.topicPartition(), lastRecord.offset);
        return lastRecord.offset == entryId;
    }

    @Override
    public boolean moveToPrevious() {
        return moveTo(lastRecord.offset - 1);
    }

    @Override
    public boolean moveToNext() {
        return moveTo(lastRecord.offset + 1);
    }

    @Override
    public Poller moveToStart() {
        if (lastRecord.first()) {
            consumer.seek(lastRecord.topicPartition(), lastRecord.offset);
        }
        return this;
    }

    @Override
    public Poller moveToEnd() {
        final long maxEndOffset = maxEndOffset();
        if (maxEndOffset == 0) {
            return moveToStart();
        }
        lastRecord.maxConsumedOffset = Math.max(lastRecord.maxConsumedOffset, maxEndOffset - 1);
        if (maxEndOffset <= lastRecord.maxConsumedOffset + 1 && lastRecord.offset != maxEndOffset) {
            lastRecord.offset = maxEndOffset;
            consumer.seek(lastRecord.topicPartition(), lastRecord.offset);
        }
        return this;
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

    private class RecordStamp {
        int partition;
        long offset;
        long maxConsumedOffset = -1;
        final Map<TopicPartition, OffsetAndMetadata> offsetMap = new Object2ObjectHashMap<>(2, DEFAULT_LOAD_FACTOR);

        void init(final ConsumerRecord<?, ?> record) {
            this.partition = record.partition();
            this.offset = record.offset();
            this.maxConsumedOffset = offset;
        }

        boolean first() {
            if (offset > 0) {
                offset = 0;
                return true;
            }
            return false;
        }

        boolean next() {
            if (offset <= maxConsumedOffset) {
                offset++;
                return true;
            }
            return false;
        }

        boolean previous() {
            if (offset > 0) {
                offset--;
                return true;
            }
            return false;
        }

        TopicPartition topicPartition() {
            return KafkaPoller.this.topicPartition(partition);
        }

        Map<TopicPartition, OffsetAndMetadata> offsetMap() {
            offsetMap.clear();
            offsetMap.put(topicPartition(), new OffsetAndMetadata(offset));
            return offsetMap;
        }
    }
}
