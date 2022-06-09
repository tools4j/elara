/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.collections.Int2ObjectHashMap;
import org.tools4j.elara.event.Event;

public class DefaultBaseState implements BaseState.Default, BaseState.Mutable {

    private final Int2ObjectHashMap<AppliedEventState> sourceToAppliedEventState = new Int2ObjectHashMap<>();

    private static final class AppliedEventState {
        long sequence;
        int index;
        void update(final long sequence, final int index) {
            this.sequence = sequence;
            this.index = index;
        }
    }

    @Override
    public boolean allEventsAppliedFor(final int source, final long sequence) {
        final AppliedEventState appliedEventState = sourceToAppliedEventState.get(source);
        if (appliedEventState != null) {
            return sequence < appliedEventState.sequence;
        }
        return false;
    }

    @Override
    public boolean eventApplied(final int source, final long sequence, final int index) {
        final AppliedEventState appliedEventState = sourceToAppliedEventState.get(source);
        if (appliedEventState != null) {
            return sequence < appliedEventState.sequence ||
                    sequence == appliedEventState.sequence && index <= appliedEventState.index;
        }
        return false;
    }

    @Override
    public Mutable applyEvent(final int source, final long sequence, final int index) {
        sourceToAppliedEventState.computeIfAbsent(source, k -> new AppliedEventState())
                .update(sequence, index);
        return this;
    }

    @Override
    public Mutable applyEvent(final Event event) {
        final Event.Id id = event.id();
        return applyEvent(id.source(), id.sequence(), id.index());
    }
}
