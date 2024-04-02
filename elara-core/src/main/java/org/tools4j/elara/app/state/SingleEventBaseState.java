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

import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.EventType;

import static java.util.Objects.requireNonNull;

/**
 * Single event base state allows only one event per command.
 */
public class SingleEventBaseState implements PassthroughState {
    public static final BaseStateProvider PROVIDER = appConfig -> new SingleEventBaseState();

    private final PassthroughState delegate;

    public SingleEventBaseState() {
        this(new DefaultBaseState());
    }

    public SingleEventBaseState(final PassthroughState delegate) {
        this.delegate = requireNonNull(delegate);
    }

    public static SingleEventBaseState create() {
        return new SingleEventBaseState();
    }

    @Override
    public long lastAppliedCommandSequence(final int sourceId) {
        return delegate.lastAppliedCommandSequence(sourceId);
    }

    @Override
    public long lastAppliedEventSequence() {
        return delegate.lastAppliedEventSequence();
    }

    @Override
    public boolean eventAppliedForCommand(final int sourceId, final long commandSeq) {
        return delegate.eventAppliedForCommand(sourceId, commandSeq);
    }

    @Override
    public boolean eventApplied(final long eventSeq) {
        return delegate.eventApplied(eventSeq);
    }

    @Override
    public void applyEvent(final Event event) {
        if (event.eventIndex() != 0) {
            throw new IllegalArgumentException("Only event with index 0 is allowed");
        }
        delegate.applyEvent(event);
    }

    @Override
    public void applyEvent(final int srcId, final long srcSeq, final long evtSeq, final int evtIndex, final EventType evtType, final long evtTime, final int payloadType) {
        if (evtIndex != 0) {
            throw new IllegalArgumentException("Only event with index 0 is allowed");
        }
        delegate.applyEvent(srcId, srcSeq, evtSeq, evtIndex, evtType, evtTime, payloadType);
    }
}
