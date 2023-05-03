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
package org.tools4j.elara.app.state;

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.app.state.BaseState.MIN_SEQUENCE;
import static org.tools4j.elara.app.state.BaseState.NIL_SEQUENCE;

/**
 * Event state returned by {@link CommandTracker#eventLastProcessed()} if {@link BaseState} is in use.
 */
public final class BaseEventState implements EventState {
    private final int sourceId;
    private final BaseState baseState;

    public BaseEventState(final int sourceId, final BaseState baseState) {
        this.sourceId = sourceId;
        this.baseState = requireNonNull(baseState);
    }

    @Override
    public int sourceId() {
        return sourceId;
    }

    @Override
    public long eventsProcessed() {
        return sourceSequence() != NIL_SEQUENCE ? 1 : 0;
    }

    @Override
    public long sourceSequence() {
        return baseState.lastAppliedCommandSequence(sourceId);
    }

    @Override
    public long eventSequence() {
        return sourceSequence() != NIL_SEQUENCE ? MIN_SEQUENCE : NIL_SEQUENCE;
    }

    @Override
    public int eventIndex() {
        return sourceSequence() != NIL_SEQUENCE ? 0 : -1;
    }

    @Override
    public EventType eventType() {
        return sourceSequence() != NIL_SEQUENCE ? EventType.APP_COMMIT : null;
    }

    @Override
    public long eventTime() {
        return TimeSource.MIN_VALUE;
    }

    @Override
    public int payloadType() {
        return 0;
    }

    @Override
    public String toString() {
        return "BaseEventState{" +
                "source-id=" + sourceId +
                "|source-seq=" + sourceSequence() +
                '}';
    }
}
