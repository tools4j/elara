/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.state;

import org.agrona.collections.Hashing;
import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.time.TimeSource;

public class DefaultEngineState implements MutableEngineState {

    public static final int DEFAULT_INITIAL_SOURCE_ID_CAPACITY = 32;
    private final Long2LongHashMap sourceIdToMaxAvailableSourceSeq;
    private long maxAvailableEventSeq;
    private long newestEventTime;

    public DefaultEngineState() {
        this(DEFAULT_INITIAL_SOURCE_ID_CAPACITY);
    }

    public DefaultEngineState(final int sourceIdInitialCapacity) {
        this.sourceIdToMaxAvailableSourceSeq = new Long2LongHashMap(sourceIdInitialCapacity, Hashing.DEFAULT_LOAD_FACTOR, BaseState.NIL_SEQUENCE);
        this.maxAvailableEventSeq = BaseState.NIL_SEQUENCE;
        this.newestEventTime = TimeSource.MIN_VALUE;
    }

    public void reset() {
        sourceIdToMaxAvailableSourceSeq.clear();
        maxAvailableEventSeq = BaseState.NIL_SEQUENCE;
        newestEventTime = TimeSource.MIN_VALUE;
    }

    @Override
    public long maxAvailableEventSequence() {
        return maxAvailableEventSeq;
    }

    @Override
    public void maxAvailableEventSequence(final long eventSeq) {
        this.maxAvailableEventSeq = eventSeq;
    }

    @Override
    public long newestEventTime() {
        return newestEventTime;
    }

    @Override
    public void newestEventTime(final long eventTime) {
        this.newestEventTime = eventTime;
    }

    @Override
    public long maxAvailableSourceSequence(final int sourceId) {
        return sourceIdToMaxAvailableSourceSeq.get(sourceId);
    }

    @Override
    public void maxAvailableSourceSequence(final int sourceId, final long sourceSeq) {
        sourceIdToMaxAvailableSourceSeq.put(sourceId, sourceSeq);
    }

    @Override
    public String toString() {
        return "DefaultEngineState" +
                "|max-avail-evt-seq=" + maxAvailableEventSeq +
                "|newest-evt-time=" + newestEventTime +
                "|max-avail-src-seq=" + sourceIdToMaxAvailableSourceSeq;
    }
}
