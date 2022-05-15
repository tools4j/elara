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
package org.tools4j.elara.samples.hash;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.init.CommandPollingMode;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver.ReceivingContext;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.metrics.Configuration;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.store.InMemoryStore;
import org.tools4j.elara.time.TimeSource;

import java.util.concurrent.atomic.AtomicLong;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.COMMAND_PROCESSED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_PERFORMED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EVENT_APPLIED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.INPUT_RECEIVED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.OUTPUT_PUBLISHED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.TimeMetric.APPLYING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.APPLYING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_SENDING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.PROCESSING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.PROCESSING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.ROUTING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.ROUTING_START_TIME;

/**
 * Rules:
 * <pre>
 *     - Commands are simply long values
 *     - Events are also long values; if current state hash is
 *         - even: then event value is same as command value
 *         - odd:  then event value is the bitwise inverse of the value
 *     - State is a simple hash of all event values
 * </pre>
 */
public class HashApplication {

    public static final int MESSAGE_LENGTH = 5 * Long.BYTES;
    public static final long NULL_VALUE = Long.MIN_VALUE;
    public static final int DEFAULT_SOURCE = 42;

    public interface State {
        long hash();
        long count();
    }

    public interface ModifiableState extends State {
        void update(long add);
    }

    public static class DefaultState implements ModifiableState {
        private long hash;
        private long count;

        @Override
        public long hash() {
            return hash;
        }

        @Override
        public void update(final long add) {
            hash = 47 * hash + add;
            count++;
        }

        @Override
        public long count() {
            return count;
        }
    }

    public static CommandProcessor commandProcessor(final State state) {
        requireNonNull(state);
        return (command, router) -> {
            if (command.isApplication()) {
                final long commandValue = command.payload().getLong(0);
                final long eventValue = isEven(state.hash()) ? commandValue : ~commandValue;
                try (final RoutingContext context = router.routingEvent()) {
                    for (int pos = 0; pos < MESSAGE_LENGTH; pos += Long.BYTES) {
                        context.buffer().putLong(pos, eventValue);
                    }
                    context.route(MESSAGE_LENGTH);
                }
            }
        };
    }

    public static EventApplier eventApplier(final ModifiableState state) {
        requireNonNull(state);
        return event -> {
            if (event.isApplication()) {
                final long value = event.payload().getLong(0);
                state.update(value);
            }
        };
    }

    public static Input input(final AtomicLong input) {
        return input(DEFAULT_SOURCE, input);
    }

    public static Input input(final int source, final AtomicLong input) {
        requireNonNull(input);
        final AtomicLong seqNo = new AtomicLong();
        return receiver -> {
            final long value = input.getAndSet(NULL_VALUE);
            if (value != NULL_VALUE) {
                final long seq = seqNo.incrementAndGet();
                try (final ReceivingContext context = receiver.receivingMessage(source, seq)) {
                    for (int pos = 0; pos < MESSAGE_LENGTH; pos += Long.BYTES) {
                        context.buffer().putLong(pos, value);
                    }
                    context.receive(MESSAGE_LENGTH);
                }
                return 1;
            }
            return 0;
        };
    }

    public static ElaraRunner inMemory(final ModifiableState state, final AtomicLong input) {
        return Elara.launch(Context.create()
                .commandProcessor(commandProcessor(state))
                .eventApplier(eventApplier(state))
                .input(input(input))
                .commandStore(new InMemoryStore())
                .eventStore(new InMemoryStore())
        );
    }

    public static ElaraRunner chronicleQueue(final ModifiableState state, final AtomicLong input) {
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash/cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                .commandProcessor(commandProcessor(state))
                .eventApplier(eventApplier(state))
                .input(input(input))
                .commandStore(new ChronicleMessageStore(cq))
                .eventStore(new ChronicleMessageStore(eq))
        );
    }

    public static ElaraRunner chronicleQueueWithMetrics(final ModifiableState state, final AtomicLong input) {
        final TimeSource pseudoNanoClock = new PseudoMicroClock();
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
//        final ChronicleQueue mq = ChronicleQueue.singleBuilder()
//                .path("build/chronicle/hash-metrics/met.cq4")
//                .wireType(WireType.BINARY_LIGHT)
//                .build();
        final ChronicleQueue tq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/tim.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue fq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/frq.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                        .commandProcessor(commandProcessor(state))
                        .eventApplier(eventApplier(state))
                        .input(input(input))
                        .commandStore(new ChronicleMessageStore(cq))
                        .eventStore(new ChronicleMessageStore(eq))
                        /* trigger capturing of output metrics approximately every second time */
                        .output((event, replay, retry, loopback) -> (event.payload().getLong(0) & 0x1) == 0 ? Ack.COMMIT : Ack.IGNORED)
                        .timeSource(pseudoNanoClock)
                        .idleStrategy(BusySpinIdleStrategy.INSTANCE)
                        .plugin(Plugins.metricsPlugin(Configuration.configure()
//                    .timeMetrics(EnumSet.allOf(TimeMetric.class))
//                    .frequencyMetrics(EnumSet.allOf(FrequencyMetric.class))
                                        .timeMetrics(INPUT_SENDING_TIME, INPUT_POLLING_TIME, PROCESSING_START_TIME, PROCESSING_END_TIME, ROUTING_START_TIME, APPLYING_START_TIME, APPLYING_END_TIME, ROUTING_END_TIME, OUTPUT_START_TIME, OUTPUT_END_TIME)
                                        .frequencyMetrics(DUTY_CYCLE_FREQUENCY, DUTY_CYCLE_PERFORMED_FREQUENCY, INPUT_RECEIVED_FREQUENCY, COMMAND_PROCESSED_FREQUENCY, EVENT_APPLIED_FREQUENCY, OUTPUT_PUBLISHED_FREQUENCY)
                                        .frequencyMetricInterval(100_000)//micros
                                        .inputSendingTimeExtractor((source, sequence, type, buffer, offset, length) -> pseudoNanoClock.currentTime() - 100_000)//for testing only
//                    .metricsStore(new ChronicleMessageStore(mq))
                                        .timeMetricsStore(new ChronicleMessageStore(tq))
                                        .frequencyMetricsStore(new ChronicleMessageStore(fq))
                        ))
        );
    }

    public static ElaraRunner chronicleQueueWithFreqMetrics(final ModifiableState state,
                                                            final AtomicLong input,
                                                            final CommandPollingMode commandPollingMode) {
        final TimeSource pseudoNanoClock = new PseudoMicroClock();
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue fq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/hash-metrics/frq.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                        .commandProcessor(commandProcessor(state))
                        .commandPollingMode(commandPollingMode)
                        .eventApplier(eventApplier(state))
                        .input(input(input))
                        .commandStore(new ChronicleMessageStore(cq))
                        .eventStore(new ChronicleMessageStore(eq))
                        /* trigger capturing of output metrics approximately every second time */
                        .output((event, replay, retry, loopback) -> (event.payload().getLong(0) & 0x1) == 0 ? Ack.COMMIT : Ack.IGNORED)
                        .timeSource(pseudoNanoClock)
                        .idleStrategy(BusySpinIdleStrategy.INSTANCE)
                        .plugin(Plugins.metricsPlugin(Configuration.configure()
                                        .frequencyMetrics(DUTY_CYCLE_FREQUENCY, DUTY_CYCLE_PERFORMED_FREQUENCY, INPUT_RECEIVED_FREQUENCY, COMMAND_PROCESSED_FREQUENCY, EVENT_APPLIED_FREQUENCY, OUTPUT_PUBLISHED_FREQUENCY)
                                        .frequencyMetricInterval(100_000)//micros
                                        .frequencyMetricsStore(new ChronicleMessageStore(fq))
                        ))
        );
    }

    private static boolean isEven(final long value) {
        return (value & 0x1) == 0;
    }
}