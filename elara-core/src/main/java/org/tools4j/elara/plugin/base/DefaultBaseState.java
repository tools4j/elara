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
package org.tools4j.elara.plugin.base;

import org.agrona.collections.Long2ObjectHashMap;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;

public class DefaultBaseState implements BaseState.Mutable {

    private final Long2ObjectHashMap<AppliedEventState> inputToAppliedEventState = new Long2ObjectHashMap<>();
    private boolean processCommands;

    private static final class AppliedEventState {
        long sequence;
        int index;
        boolean isFinal;
        void update(final Event event) {
            final Event.Id id = event.id();
            sequence = id.commandId().sequence();
            index = id.index();
            isFinal = event.flags().isFinal();
        }
    }

    public DefaultBaseState() {
        this(true);
    }

    public DefaultBaseState(final boolean processCommands) {
        this.processCommands = processCommands;
    }

    @Override
    public boolean processCommands() {
        return processCommands;
    }

    @Override
    public Mutable processCommands(final boolean newValue) {
        this.processCommands = newValue;
        return this;
    }

    @Override
    public boolean allEventsAppliedFor(final Command.Id id) {
        final AppliedEventState appliedEventState = inputToAppliedEventState.get(id.input());
        if (appliedEventState != null) {
            final long sequence = id.sequence();
            return sequence < appliedEventState.sequence ||
                    sequence == appliedEventState.sequence && appliedEventState.isFinal;
        }
        return false;
    }

    @Override
    public boolean eventApplied(final Event.Id id) {
        final Command.Id cid = id.commandId();
        final AppliedEventState appliedEventState = inputToAppliedEventState.get(cid.input());
        if (appliedEventState != null) {
            final long sequence = cid.sequence();
            return sequence < appliedEventState.sequence ||
                    sequence == appliedEventState.sequence && id.index() <= appliedEventState.index;
        }
        return false;
    }

    @Override
    public Mutable eventApplied(final Event event) {
        inputToAppliedEventState.computeIfAbsent(event.id().commandId().input(), k -> new AppliedEventState())
                .update(event);
        return this;
    }
}
