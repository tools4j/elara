/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.samples.replication;

import org.tools4j.elara.samples.network.ServerTopology;
import org.tools4j.nobark.run.RunnableFactory.RunningCondition;
import org.tools4j.nobark.run.StoppableThread;
import org.tools4j.nobark.run.ThreadLike;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.hash.HashApplication.NULL_VALUE;

public class MulticastSource {

    private final int sourceIndex;
    private final LongSupplier valueSource;
    private final ServerTopology topology;

    private MulticastSource(final int sourceIndex,
                            final LongSupplier valueSource,
                            final ServerTopology topology) {
        if (sourceIndex < 0 || sourceIndex >= topology.senders()) {
            throw new IllegalArgumentException("Invalid source index " + sourceIndex + ", must be in [0, " +
                    (topology.senders() - 1) + "]");
        }
        this.sourceIndex = sourceIndex;
        this.valueSource = requireNonNull(valueSource);
        this.topology = requireNonNull(topology);
    }

    public static ThreadLike startRandom(final int source,
                                         final IdMapping sourceIds,
                                         final int nValues,
                                         final ServerTopology topology) {
        return start(source, sourceIds, randomValueSupplier(), nValues, topology);
    }

    public static ThreadLike start(final int source,
                                   final IdMapping sourceIds,
                                   final LongSupplier valueSource,
                                   final int nValues,
                                   final ServerTopology topology) {
        if (sourceIds.count() != topology.senders()) {
            throw new IllegalArgumentException("Sources are senders, but " + sourceIds.count() + " is not equal to " +
                    topology.senders());
        }
        final MulticastSource ms = new MulticastSource(sourceIds.indexById(source), valueSource, topology);
        return StoppableThread.start(runningCondition -> () -> ms.run(runningCondition, nValues),
                r -> new Thread(null, r, "source-" + source));
    }

    private void run(final RunningCondition runningCondition, final int n) {
        for (int i = 0; i < n; i++) {
            if (!runningCondition.keepRunning()) {
                return;
            }
            final long value = valueSource.getAsLong();
            for (int receiver = 0; receiver < topology.receivers(); receiver++) {
                while (!topology.transmit(sourceIndex, receiver, value)) {
                    if (!runningCondition.keepRunning()) {
                        return;
                    }
                }
            }
        }
    }

    private static LongSupplier randomValueSupplier() {
        return () -> {
            long value;
            do {
                value = ThreadLocalRandom.current().nextLong();;
            } while (value == NULL_VALUE);
            return value;
        };
    }
}
