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
package org.tools4j.elara.source;

import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.InFlightState;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

final class DefaultInFlightState implements InFlightState {

    private final int sourceId;
    private final TimeSource timeSource;

    private final CommandSendingState commandSendingState = new CommandSendingStateImpl();
    private final EventProcessingState eventProcessingState;

    public DefaultInFlightState(final int sourceId,
                                final BaseState baseState,
                                final TimeSource timeSource) {
        this.sourceId = sourceId;
        this.timeSource = requireNonNull(timeSource);
        this.eventProcessingState = new EventProcessingStateImpl(baseState);
    }

    @Override
    public CommandSendingState commandLastSent() {
        return commandSendingState;
    }

    @Override
    public EventProcessingState eventLastProcessed() {
        return eventProcessingState;
    }

    @Override
    public boolean hasInFlightCommand() {
        return false;
    }

    private static class CommandSendingStateImpl implements CommandSendingState {
        long lastCommandSequence = NIL_SEQUENCE;
        long lastCommandSendingTime = TimeSource.MIN_VALUE;

        @Override
        public long sourceSequence() {
            return lastCommandSequence;
        }

        @Override
        public long sendingTime() {
            return lastCommandSendingTime;
        }

        @Override
        public String toString() {
            return "CommandSendingState{" +
                    "sourceSequence=" + lastCommandSequence +
                    "|sendingTime=" + lastCommandSendingTime +
                    '}';
        }
    }

    private static final class EventProcessingStateImpl implements EventProcessingState {
        final BaseState baseState;

        EventProcessingStateImpl(final BaseState baseState) {
            this.baseState = requireNonNull(baseState);
        }

        @Override
        public long sourceSequence() {
            return 0;
        }

        @Override
        public long eventSequence() {
            return 0;
        }

        @Override
        public int eventIndex() {
            return 0;
        }

        @Override
        public EventType eventType() {
            return null;
        }
    }
}
