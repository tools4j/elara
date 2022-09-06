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
import org.agrona.IoUtil;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.tools4j.elara.app.type.PassthroughApp;
import org.tools4j.elara.app.type.PublisherApp;
import org.tools4j.elara.app.type.PublisherAppContext;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.base.BasePlugin;
import org.tools4j.elara.plugin.base.SingleEventBaseState;
import org.tools4j.elara.plugin.metrics.MetricsConfig;
import org.tools4j.elara.plugin.metrics.MetricsContext;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.hash.HashApplication.ModifiableState;
import org.tools4j.elara.time.TimeSource;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_PERFORMED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.INPUT_RECEIVED_FREQUENCY;
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
import static org.tools4j.elara.samples.hash.HashApplication.isEven;

public class HashPassthroughApplication implements PassthroughApp {
    //nothing to add

    public static class PublisherWithState implements PublisherApp {
        final ModifiableState state;

        public PublisherWithState(final ModifiableState state) {
            this.state = requireNonNull(state);
        }

        @Override
        public Ack publish(final Event event, final boolean replay, final int retry) {
            final long eventValue = event.payload().getLong(0);
            final long updateValue = isEven(state.hash()) ? eventValue : ~eventValue;
            state.update(updateValue);
            return Ack.COMMIT;
        }
    }

    public static ElaraRunner chronicleQueueWithMetrics(final String folder, final AtomicLong input) {
        return chronicleQueueWithMetrics(folder, input, true);
    }

    public static ElaraRunner chronicleQueueWithFreqMetrics(final String folder, final AtomicLong input) {
        return chronicleQueueWithMetrics(folder, input, false);
    }

    private static ElaraRunner chronicleQueueWithMetrics(final String folder,
                                                         final AtomicLong input,
                                                         final boolean timeMetrics) {
        final String path = "build/chronicle/" + folder;
        IoUtil.delete(new File(path), true);
        final TimeSource pseudoNanoClock = new PseudoMicroClock();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path(path + "/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue tq = timeMetrics ? ChronicleQueue.singleBuilder()
                .path(path + "/tim.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build() : null;
        final ChronicleQueue fq = ChronicleQueue.singleBuilder()
                .path(path + "/frq.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final MetricsContext metricsConfig = MetricsConfig.configure()
//                .frequencyMetrics(DUTY_CYCLE_FREQUENCY, DUTY_CYCLE_PERFORMED_FREQUENCY, INPUT_RECEIVED_FREQUENCY, COMMAND_PROCESSED_FREQUENCY, EVENT_APPLIED_FREQUENCY, OUTPUT_PUBLISHED_FREQUENCY)
                .frequencyMetrics(DUTY_CYCLE_FREQUENCY, DUTY_CYCLE_PERFORMED_FREQUENCY, INPUT_RECEIVED_FREQUENCY)
                .frequencyMetricInterval(100_000)//micros
                .frequencyMetricsStore(new ChronicleMessageStore(fq));
        if (timeMetrics) metricsConfig
                .timeMetrics(INPUT_SENDING_TIME, INPUT_POLLING_TIME, PROCESSING_START_TIME, PROCESSING_END_TIME, ROUTING_START_TIME, APPLYING_START_TIME, APPLYING_END_TIME, ROUTING_END_TIME, OUTPUT_START_TIME, OUTPUT_END_TIME)
                .inputSendingTimeExtractor((source, sequence, type, buffer, offset, length) -> pseudoNanoClock.currentTime() - 100_000)//for testing only
                .timeMetricsStore(new ChronicleMessageStore(tq));
        return new HashPassthroughApplication().launch(context -> context
                .input(HashApplication.input(input))
                .eventStore(new ChronicleMessageStore(eq))
                .timeSource(pseudoNanoClock)
                .idleStrategy(BusySpinIdleStrategy.INSTANCE)
                .plugin(Plugins.metricsPlugin(metricsConfig))
        );
    }

    public static ElaraRunner publisherWithState(final String folder, final ModifiableState state) {
        final String path = "build/chronicle/" + folder;
        final TimeSource pseudoNanoClock = new PseudoMicroClock();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path(path + "/evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return new PublisherWithState(state).launch((Consumer<? super PublisherAppContext>)context -> context
                .eventStore(new ChronicleMessageStore(eq))
                .timeSource(pseudoNanoClock)
                .idleStrategy(BusySpinIdleStrategy.INSTANCE)
                .plugin(BasePlugin.INSTANCE, SingleEventBaseState::new)
        );
    }
}
