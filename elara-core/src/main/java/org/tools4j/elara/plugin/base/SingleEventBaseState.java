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
package org.tools4j.elara.plugin.base;

import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.event.Event;

/**
 * Single event base state allows only one event per command.
 */
public class SingleEventBaseState implements BaseState.Mutable {

    private static final long MISSING_SEQUENCE = Long.MIN_VALUE;
    private final Long2LongHashMap sourceToSequence = new Long2LongHashMap(MISSING_SEQUENCE);

    @Override
    public boolean allEventsAppliedFor(final int sourceId, final long sourceSeq) {
        final long appliedSequence = sourceToSequence.get(sourceId);
        return sourceSeq <= appliedSequence && appliedSequence != MISSING_SEQUENCE;
    }

    @Override
    public boolean eventApplied(final int sourceId, final long sourceSeq, final int index) {
        return allEventsAppliedFor(sourceId, sourceSeq);
    }

    @Override
    public Mutable applyEvent(final Event event) {
        return applyEvent(event.sourceId(), event.sourceSequence(), event.index());
    }

    @Override
    public Mutable applyEvent(final int sourceId, final long sourceSeq, final int index) {
        if (index != 0) {
            throw new IllegalArgumentException("Only event with index 0 is allowed");
        }
        sourceToSequence.put(sourceId, sourceSeq);
        return this;
    }
}
