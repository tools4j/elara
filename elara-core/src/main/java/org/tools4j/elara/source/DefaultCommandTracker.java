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

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.app.state.BaseEventState;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.EventProcessingState.MutableEventProcessingState;
import org.tools4j.elara.app.state.EventInfo;
import org.tools4j.elara.app.state.TransientCommandState;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.sequence.SequenceGenerator;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

final class DefaultCommandTracker implements CommandTracker {

    private final SourceContext sourceContext;
    private final SequenceGenerator sourceSequenceGenerator;
    private final EventInfo eventLastProcessed;
    private final TransientCommandStateImpl transientCommandState = new TransientCommandStateImpl();

    public DefaultCommandTracker(final SourceContext sourceContext,
                                 final SequenceGenerator sourceSequenceGenerator,
                                 final BaseState baseState) {
        this.sourceContext = requireNonNull(sourceContext);
        this.sourceSequenceGenerator = requireNonNull(sourceSequenceGenerator);
        this.eventLastProcessed = baseState instanceof MutableEventProcessingState ?
                ((MutableEventProcessingState)baseState).lastProcessedEventCreateIfAbsent(sourceContext.sourceId()) :
                new BaseEventState(sourceContext.sourceId(), baseState);
    }

    @Override
    public int sourceId() {
        return sourceContext.sourceId();
    }

    @Override
    public TransientCommandState transientCommandState() {
        return transientCommandState;
    }

    @Override
    public EventInfo eventLastProcessed() {
        return eventLastProcessed;
    }

    @Override
    public boolean hasInFlightCommand() {
        final long cmdSeq = transientCommandState.sourceSequenceOfLastSentCommand();
        final long evtSeq = eventLastProcessed.sourceSequence();
        return (evtSeq < cmdSeq) || (evtSeq == cmdSeq && eventsPending(eventLastProcessed.eventType()));
    }

    private static boolean eventsPending(final EventType eventType) {
        return eventType != null && !eventType.isLast();
    }

    void notifyCommandSent(final long sourceSequence, final long commandTime) {
        assert sourceSequence > transientCommandState.lastCommandSequence;
        transientCommandState.commandsSent++;
        transientCommandState.lastCommandSequence = sourceSequence;
        transientCommandState.lastCommandSendingTime = commandTime;
    }

    private class TransientCommandStateImpl implements TransientCommandState {
        long commandsSent = 0;
        long lastCommandSequence = NIL_SEQUENCE;
        long lastCommandSendingTime = TimeSource.MIN_VALUE;

        @Override
        public int sourceId() {
            return sourceContext.sourceId();
        }

        @Override
        public SequenceGenerator sourceSequenceGenerator() {
            return sourceSequenceGenerator;
        }

        @Override
        public long commandsSent() {
            return commandsSent;
        }

        @Override
        public long sourceSequenceOfLastSentCommand() {
            return lastCommandSequence;
        }

        @Override
        public long sendingTimeOfLastSentCommand() {
            return lastCommandSendingTime;
        }

        @Override
        public String toString() {
            return "TransientCommandState{" +
                    "sourceId=" + sourceId() +
                    "|commandsSent=" + commandsSent +
                    "|sourceSequenceOfLastSentCommand=" + lastCommandSequence +
                    "|sendingTimeOfLastSentCommand=" + lastCommandSendingTime +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "DefaultCommandTracker{" +
                "sourceId=" + sourceId() +
                "|hasInFlightCommand=" + hasInFlightCommand() +
                "|transientCommandState=" + transientCommandState +
                "|eventLastProcessed=" + eventLastProcessed +
                '}';
    }
}
