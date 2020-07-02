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
import java.util.function.LongSupplier;

public interface NetworkConfig {

    NetworkConfig IDEAL = new NetworkConfig() {};
    NetworkConfig DEFAULT = new NetworkConfig() {
        @Override
        public LinkConfig commandLink() {
            return LinkConfig.DEFAULT;
        }

        @Override
        public LinkConfig appendLink() {
            return LinkConfig.DEFAULT;
        }
    };

    interface LinkConfig {
        LinkConfig IDEAL = new LinkConfig() {};
        LinkConfig DEFAULT = real(0, 100, 0.01f);

        static LinkConfig real(final long minDelayNanos, final long maxDelayNanos, final float lossRatio) {
            return new LinkConfig() {
                @Override
                public LongSupplier transmissionDelayNanos() {
                    if (minDelayNanos >= maxDelayNanos) {
                        return () -> maxDelayNanos;
                    }
                    return () -> ThreadLocalRandom.current().nextLong(minDelayNanos, maxDelayNanos);
                }

                @Override
                public float transmissionLossRatio() {
                    return lossRatio;
                }
            };
        }

        default LongSupplier transmissionDelayNanos() {
            return () -> 0;
        }
        default float transmissionLossRatio() {
            return 0;
        }
        default int senderBufferCapacity() {
            return 100;
        }
        default int receiverBufferCapacity() {
            return 100;
        }
    }

    default LinkConfig commandLink() {
        return LinkConfig.IDEAL;
    }
    default LinkConfig appendLink() {
        return LinkConfig.IDEAL;
    }
}
