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

import org.agrona.collections.Int2ObjectHashMap;
import org.tools4j.elara.app.state.EventProcessingState.MutableEventProcessingState;
import org.tools4j.elara.flyweight.EventType;

public class DefaultEventProcessingState implements MutableEventProcessingState, PassthroughState {
    public static final BaseStateProvider PROVIDER = appConfig -> new DefaultEventProcessingState();

    private final Int2ObjectHashMap<DefaultEventState> sourceIdToEventState = new Int2ObjectHashMap<>();
    private long lastAppliedEventSequence = NIL_SEQUENCE;

    @Override
    public long lastAppliedCommandSequence(final int sourceId) {
        final EventState eventState = lastProcessedEvent(sourceId);
        return eventState != null ? eventState.sourceSequence() : NIL_SEQUENCE;
    }

    @Override
    public long lastAppliedEventSequence() {
        return lastAppliedEventSequence;
    }

    @Override
    public EventState lastProcessedEvent(final int sourceId) {
        return sourceIdToEventState.get(sourceId);
    }

    @Override
    public DefaultEventState lastProcessedEventCreateIfAbsent(final int sourceId) {
        return sourceIdToEventState.computeIfAbsent(sourceId, DefaultEventState::new);
    }

    @Override
    public void applyEvent(final int srcId, final long srcSeq, final long evtSeq, final int evtIndex,
                           final EventType evtType, final long evtTime, final int payloadType) {
        lastProcessedEventCreateIfAbsent(srcId).applyEvent(srcSeq, evtSeq, evtIndex, evtType, evtTime, payloadType);
    }

    @Override
    public String toString() {
        return "DefaultEventProcessingState{" +
                "sourceIdToEventState=" + sourceIdToEventState +
                "|lastAppliedEventSequence=" + lastAppliedEventSequence +
                '}';
    }
}
