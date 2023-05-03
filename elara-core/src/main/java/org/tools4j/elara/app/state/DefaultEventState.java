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

import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.time.TimeSource;

final class DefaultEventState implements EventState {
    private final int sourceId;
    private long count;
    private long sourceSequence = BaseState.NIL_SEQUENCE;
    private long eventSequence = BaseState.NIL_SEQUENCE;
    private int eventIndex = -1;
    private EventType eventType;
    private long eventTime = TimeSource.MIN_VALUE;
    private int payloadType;

    DefaultEventState(final int sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public int sourceId() {
        return sourceId;
    }

    @Override
    public long eventsProcessed() {
        return count;
    }

    @Override
    public long sourceSequence() {
        return sourceSequence;
    }

    @Override
    public long eventSequence() {
        return eventSequence;
    }

    @Override
    public int eventIndex() {
        return eventIndex;
    }

    @Override
    public EventType eventType() {
        return eventType;
    }

    @Override
    public long eventTime() {
        return eventTime;
    }

    @Override
    public int payloadType() {
        return payloadType;
    }

    void applyEvent(final long srcSeq, final long evtSeq, final int evtIndex, final EventType evtType,
                    final long evtTime, final int payloadType) {
        this.count++;
        this.sourceSequence = srcSeq;
        this.eventSequence = evtSeq;
        this.eventIndex = evtIndex;
        this.eventType = evtType;
        this.eventTime = evtTime;
        this.payloadType = payloadType;
    }

    @Override
    public String toString() {
        return "EventState{" +
                "source-id=" + sourceId +
                "|events-processed=" + count +
                "|source-seq=" + sourceSequence +
                "|event-seq=" + eventSequence +
                "|event-index=" + eventIndex +
                "|event=type=" + eventType +
                "|event-time=" + eventTime +
                "|payload-type=" + payloadType +
                '}';
    }
}
