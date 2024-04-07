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
package org.tools4j.elara.source;

import org.tools4j.elara.app.state.EventState;
import org.tools4j.elara.app.state.MutableInFlightState;
import org.tools4j.elara.app.state.TransientCommandSourceState;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.send.SenderSupplier.SentListener;
import org.tools4j.elara.sequence.SequenceGenerator;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

final class DefaultCommandSource implements CommandSource {

    private final int sourceId;
    private final SenderSupplier senderSupplier;
    private final SequenceGenerator sequenceGenerator;
    private final EventState eventLastProcessed;
    private final MutableInFlightState inFlightState;
    private final TransientCommandSourceState transientCommandState = new TransientCommandStateImpl();
    private final SentListener sentListener = (TransientCommandStateImpl)transientCommandState;

    public DefaultCommandSource(final int sourceId,
                                final SenderSupplier senderSupplier,
                                final EventState eventState,
                                final MutableInFlightState inFlightState) {
        this(sourceId, senderSupplier, SequenceGenerator.create(), eventState, inFlightState);
    }

    public DefaultCommandSource(final int sourceId,
                                final SenderSupplier senderSupplier,
                                final SequenceGenerator sourceSequenceGenerator,
                                final EventState eventState,
                                final MutableInFlightState inFlightState) {
        this.sourceId = sourceId;
        this.senderSupplier = requireNonNull(senderSupplier);
        this.sequenceGenerator = requireNonNull(sourceSequenceGenerator);
        this.eventLastProcessed = requireNonNull(eventState);
        this.inFlightState = requireNonNull(inFlightState);
    }

    @Override
    public int sourceId() {
        return sourceId;
    }

    @Override
    public CommandSender commandSender() {
        return senderSupplier.senderFor(this, sentListener);
    }

    @Override
    public TransientCommandSourceState transientCommandSourceState() {
        return transientCommandState;
    }

    @Override
    public EventState eventLastProcessed() {
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

    private class TransientCommandStateImpl implements TransientCommandSourceState, SentListener {
        long commandsSent = 0;
        long lastCommandSequence = NIL_SEQUENCE;
        long lastCommandSendingTime = TimeSource.MIN_VALUE;

        @Override
        public int sourceId() {
            return sourceId;
        }

        @Override
        public SequenceGenerator sourceSequenceGenerator() {
            return sequenceGenerator;
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
        public void onSent(final long sourceSequence, final long commandTime) {
            assert sourceSequence > lastCommandSequence;
            commandsSent++;
            lastCommandSequence = sourceSequence;
            lastCommandSendingTime = commandTime;
            sequenceGenerator.nextSequence();
            inFlightState.onCommandSent(sourceId, sourceSequence, commandTime);
        }

        @Override
        public String toString() {
            return "TransientCommandState" +
                    ":source-id=" + sourceId +
                    "|commands-sent=" + commandsSent +
                    "|source-seq-of-last-sent-cmd=" + lastCommandSequence +
                    "|sending-time-of-last-sent-cmd=" + lastCommandSendingTime;
        }
    }

    @Override
    public String toString() {
        return "DefaultCommandSource" +
                ":source-id=" + sourceId +
                "|has-in-flight-command=" + hasInFlightCommand() +
                "|transient-cmd-state=" + transientCommandState +
                "|evt-last-processed=" + eventLastProcessed;
    }
}
