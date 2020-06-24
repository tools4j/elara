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

import org.tools4j.nobark.run.RunnableFactory.RunningCondition;
import org.tools4j.nobark.run.StoppableThread;
import org.tools4j.nobark.run.ThreadLike;

import java.util.function.LongSupplier;

import static java.util.Objects.requireNonNull;

public class MulticastPublisher {

    private final LongSupplier valueSource;
    private final int n;
    private final Channel[] destinations;

    private MulticastPublisher(final LongSupplier valueSource, final int n, final Channel... destinations) {
        this.valueSource = requireNonNull(valueSource);
        this.n = n;
        this.destinations = requireNonNull(destinations);
    }

    public static ThreadLike start(final String name, final LongSupplier valueSource, final int n, final Channel... destinations) {
        final MulticastPublisher publisher = new MulticastPublisher(valueSource, n, destinations);
        return StoppableThread.start(runningCondition -> () -> publisher.run(runningCondition),
                r -> new Thread(null, r, name));
    }

    private void run(final RunningCondition runningCondition) {
        for (int i = 0; i < n; i++) {
            if (!runningCondition.keepRunning()) {
                return;
            }
            final long value = valueSource.getAsLong();
            for (final Channel destination : destinations) {
                while (!destination.offer(value)) {
                    if (!runningCondition.keepRunning()) {
                        return;
                    }
                }
            }
        }
    }
}
