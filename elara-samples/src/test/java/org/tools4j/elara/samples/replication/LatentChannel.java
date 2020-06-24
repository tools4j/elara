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

import java.util.concurrent.ThreadLocalRandom;

import static java.util.Objects.requireNonNull;

/**
 * A transmission channel that incurs random delays loss.
 */
public class LatentChannel implements Channel {

    private final Channel destination;
    private final long delayMinMillis;//inclusive
    private final long delayMaxMillis;//exclusive

    public LatentChannel(final Channel destination, final long delayMinMillis, final long delayMaxMillis) {
        this.destination = requireNonNull(destination);
        this.delayMinMillis = delayMinMillis;
        this.delayMaxMillis = delayMaxMillis;
    }

    @Override
    public boolean offer(final long value) {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final long delayMillis = random.nextLong(delayMinMillis, delayMaxMillis);
        if (delayMillis > 0) {
            sleep(delayMillis);
        }
        return destination.offer(value);
    }

    private static void sleep(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            //don't care
        }
    }
}
