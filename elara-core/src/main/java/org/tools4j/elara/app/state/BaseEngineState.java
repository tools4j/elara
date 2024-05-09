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

import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

/**
 * Basic implementation of {@link MutableEngineState} for cases where engine state does not need to be tracked (because
 * we are the engine), or where engine tracking is not done for performance reasons. All available information is based
 * solely on the {@link BaseState}.
 */
public final class BaseEngineState implements MutableEngineState {

    private final BaseState baseState;

    public BaseEngineState(final BaseState baseState) {
        this.baseState = requireNonNull(baseState);
    }

    @Override
    public long maxAvailableEventSequence() {
        return baseState.lastAppliedEventSequence();
    }

    @Override
    public void maxAvailableEventSequence(final long eventSeq) {
        //no-op
    }

    @Override
    public long maxAvailableSourceSequence(final int sourceId) {
        return baseState.lastAppliedCommandSequence(sourceId);
    }

    @Override
    public void maxAvailableSourceSequence(final int sourceId, final long sourceSeq) {
        //no-op
    }

    @Override
    public long newestEventTime() {
        return TimeSource.MIN_VALUE;
    }

    @Override
    public void newestEventTime(final long eventTime) {
        //no-op
    }
}
