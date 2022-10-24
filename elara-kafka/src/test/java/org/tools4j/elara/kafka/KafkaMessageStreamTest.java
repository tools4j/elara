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

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.streams.integration.utils.EmbeddedKafkaCluster;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.MessageStreamTest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

class KafkaMessageStreamTest extends MessageStreamTest {

    private static final String TOPIC = "elara-stream";

    private static EmbeddedKafkaCluster cluster;

    static void assumeNotWindows() {
        final String osName = System.getProperty("os.name");
        assumeFalse(osName.toLowerCase().contains("win"), "Test is currently supported on windows, os=" + osName);
    }

    @BeforeAll
    static void startKafkaCluster() throws InterruptedException, IOException {
        assumeNotWindows();
        cluster = new EmbeddedKafkaCluster(1);
        cluster.start();
        cluster.createTopic(TOPIC);
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

    @ParameterizedTest(name = "sendAndReceiveMessages: {0} --> {1}")
    @MethodSource("kafkaSendersAndReceivers")
    @Override
    protected void sendAndReceiveMessages(final MessageSender sender, final MessageReceiver receiver) throws Exception {
        super.sendAndReceiveMessages(sender, receiver);
    }

    static Arguments[] kafkaSendersAndReceivers() {
        return new Arguments[]{
                kafkaSenderAndReceiver()
        };
    }

    private static Arguments kafkaSenderAndReceiver() {
        final AtomicLong keyGenerator = new AtomicLong(System.currentTimeMillis());
        final Kafka kafka = new Kafka(kafkaConfig());
        return Arguments.of(
                kafka.sender(TOPIC, message -> keyGenerator.incrementAndGet(), new LongSerializer()),
                kafka.receiver(TOPIC, new LongDeserializer())
        );
    }

    private static Map<String, Object> kafkaConfig() {
        final Map<String, Object> config = new LinkedHashMap<>();
        config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, cluster.bootstrapServers());
        config.put(ProducerConfig.RETRIES_CONFIG, 0);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-message-stream-test-consumer");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return config;
    }
}