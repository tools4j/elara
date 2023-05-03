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
package org.tools4j.elara.plugin.timer;

import org.agrona.collections.Hashing;
import org.agrona.collections.Int2IntHashMap;

/**
 * Generates collision free timer IDs for different sources IDs.
 * The timer ID range per source ID is that of an integer.
 */
public final class TimerIdGenerator {
    public static final int DEFAULT_SOURCE_ID_CAPACITY = 64;

    private final Int2IntHashMap sequenceBySourceId;

    public TimerIdGenerator() {
        this(DEFAULT_SOURCE_ID_CAPACITY);
    }
    public TimerIdGenerator(final int initialSourceIdCapacity) {
        this.sequenceBySourceId = new Int2IntHashMap(initialSourceIdCapacity, Hashing.DEFAULT_LOAD_FACTOR, 1);
    }

    public long current(final int sourceId) {
        final int sequence = sequenceBySourceId.get(sourceId);
        return timerId(sourceId, sequence);
    }

    public boolean next(final long minNext) {
        final int sourceId = sourceId(minNext);
        final long sequence = current(sourceId);
        if (minNext > sequence) {
            final int minSequence = sequence(minNext);
            sequenceBySourceId.put(sourceId, minSequence);
            return true;
        }
        return sequence == minNext;
    }

    public static long timerId(final int sourceId, final int sequence) {
        return (Integer.toUnsignedLong(sourceId) << 32) | Integer.toUnsignedLong(sequence);
    }

    public static int sourceId(final long timerId) {
        return (int)(timerId >>> 32);
    }

    public static int sequence(final long timerId) {
        return (int)timerId;
    }

}
