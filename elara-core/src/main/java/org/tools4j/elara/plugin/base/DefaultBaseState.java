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

import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.time.TimeSource;

public class DefaultBaseState implements BaseState.Mutable {

    private final Long2LongHashMap inputToLastCommandAllEventsApplied = new Long2LongHashMap(NO_COMMANDS);
    private boolean processCommands;
    private boolean allEventsPolled;
    private long lastAppliedEventTime = TimeSource.BIG_BANG;

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
    public boolean allEventsPolled() {
        return allEventsPolled;
    }

    @Override
    public Mutable allEventsPolled(final boolean newValue) {
        this.allEventsPolled = newValue;
        return this;
    }

    @Override
    public long lastCommandAllEventsApplied(final int input) {
        return inputToLastCommandAllEventsApplied.get(input);
    }

    @Override
    public Mutable allEventsAppliedFor(final Command.Id id) {
        assert id.sequence() > inputToLastCommandAllEventsApplied.get(id.input());
        inputToLastCommandAllEventsApplied.put(id.input(), id.sequence());
        return this;
    }

    @Override
    public long lastAppliedEventTime() {
        return lastAppliedEventTime;
    }

    @Override
    public Mutable lastAppliedEvent(final Event event) {
        final long time = event.time();
        assert lastAppliedEventTime <= time;
        lastAppliedEventTime = time;
        return this;
    }
}
