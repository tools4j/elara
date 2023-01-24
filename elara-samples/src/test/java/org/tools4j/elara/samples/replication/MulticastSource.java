/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.network.ServerTopology;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.hash.HashApplication.NULL_VALUE;

public class MulticastSource {

    private final int sourceIndex;
    private final LongSupplier valueSource;
    private final ServerTopology topology;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[Long.BYTES]);

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

    public static ElaraRunner startRandom(final int source,
                                          final IdMapping sourceIds,
                                          final int nValues,
                                          final ServerTopology topology) {
        return start(source, sourceIds, randomValueSupplier(), nValues, topology);
    }

    public static ElaraRunner start(final int source,
                                    final IdMapping sourceIds,
                                    final LongSupplier valueSource,
                                    final int nValues,
                                    final ServerTopology topology) {
        if (sourceIds.count() != topology.senders()) {
            throw new IllegalArgumentException("Sources are senders, but " + sourceIds.count() + " is not equal to " +
                    topology.senders());
        }
        final MulticastSource ms = new MulticastSource(sourceIds.indexById(source), valueSource, topology);
        final AgentRunner agentRunner = ms.agentRunner("source-" + source, nValues);
        return ElaraRunner.startOnThread(agentRunner);
    }

    private AgentRunner agentRunner(final String roleName, final int n) {
        requireNonNull(roleName);
        final AgentRunner[] runnerPtr = new AgentRunner[1];
        final Agent agent = new Agent() {
            int index = 0;
            int receiver = topology.receivers();

            @Override
            public int doWork() {
                int workDone = 0;
                while (receiver < topology.receivers()) {
                    if (!topology.transmit(sourceIndex, receiver, buffer, 0, Long.BYTES)) {
                        return workDone;
                    }
                    workDone++;
                    receiver++;
                }
                if (index < n) {
                    buffer.putLong(0, valueSource.getAsLong());
                    index++;
                    receiver = 0;
                    workDone++;
                } else {
                    runnerPtr[0].close();
                }
                return workDone;
            }

            @Override
            public String roleName() {
                return roleName;
            }
        };
        return runnerPtr[0] = new AgentRunner(new BackoffIdleStrategy(), Throwable::printStackTrace, null, agent);
    }

    private static LongSupplier randomValueSupplier() {
        return () -> {
            long value;
            do {
                value = ThreadLocalRandom.current().nextLong();
            } while (value == NULL_VALUE);
            return value;
        };
    }
}
