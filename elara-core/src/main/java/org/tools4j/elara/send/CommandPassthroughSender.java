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
package org.tools4j.elara.send;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.PassthroughEventApplier;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.CommandDescriptor;
import org.tools4j.elara.flyweight.EventDescriptor;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.store.ExpandableDirectBuffer;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

/**
 * A command sender that directly invokes the passthrough command handler without persisting the command, instead
 * directly routing an event with the payload.
 */
public final class CommandPassthroughSender extends FlyweightCommandSender {

    private final TimeSource timeSource;
    private final BaseState baseState;
    private final ExceptionHandler exceptionHandler;
    private final DuplicateHandler duplicateHandler;
    private final MessageStore.Appender eventStoreAppender;
    private final EventApplier eventApplier;
    private final PassthroughEventApplier passthroughApplierOrNull;
    private final SendingContext sendingContext = new SendingContext();
    private final FlyweightCommand skippedCommand = new FlyweightCommand();
    private final FlyweightEvent appliedEvent = new FlyweightEvent();

    public CommandPassthroughSender(final TimeSource timeSource,
                                    final BaseState baseState,
                                    final MessageStore.Appender eventStoreAppender,
                                    final EventApplier eventApplier,
                                    final ExceptionHandler exceptionHandler,
                                    final DuplicateHandler duplicateHandler) {
        this.timeSource = requireNonNull(timeSource);
        this.baseState = requireNonNull(baseState);
        this.eventStoreAppender = requireNonNull(eventStoreAppender);
        this.eventApplier = requireNonNull(eventApplier);
        this.passthroughApplierOrNull = eventApplier instanceof PassthroughEventApplier ? (PassthroughEventApplier)eventApplier : null;
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.duplicateHandler = requireNonNull(duplicateHandler);
    }

    private long nextEventSequence() {
        return baseState.lastAppliedEventSequence() + 1;
    }

    @Override
    public CommandSender.SendingContext sendingCommand(final int payloadType) {
        return sendingContext.init(sourceId(), nextCommandSequence(), nextEventSequence(), payloadType, eventStoreAppender.appending());
    }

    private final class SendingContext implements CommandSender.SendingContext {

        private static final short INDEX_0 = 0;
        final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        AppendingContext context;

        SendingContext init(final int sourceId, final long sourceSeq, final long eventSeq,
                            final int payloadType, final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), EventDescriptor.PAYLOAD_OFFSET);
            FlyweightEvent.writeHeader(
                    EventType.APP_COMMIT, sourceId, sourceSeq, INDEX_0, eventSeq, timeSource.currentTime(), payloadType, 0,
                    context.buffer(), EventDescriptor.HEADER_OFFSET
            );
            return this;
        }

        AppendingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Sending context is closed");
        }

        @Override
        public int sourceId() {
            return FlyweightEvent.sourceId(unclosedContext().buffer());
        }

        @Override
        public long sourceSequence() {
            return FlyweightEvent.sourceSequence(unclosedContext().buffer());
        }

        private long eventSequence() {
            return FlyweightEvent.sourceSequence(unclosedContext().buffer());
        }

        @Override
        public MutableDirectBuffer buffer() {
            //noinspection ResultOfMethodCallIgnored
            unclosedContext();
            return buffer;
        }

        @Override
        public SendingResult send(final int length) {
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            buffer.unwrap();
            try (final AppendingContext ac = unclosedContext()) {
                if (length > 0) {
                    FlyweightEvent.writePayloadSize(length, ac.buffer());
                }
                if (baseState.eventAppliedForCommand(sourceId(), sourceSequence())) {
                    skipCommand(ac.buffer(), length);
                } else {
                    applyEvent(ac.buffer(), length);
                    ac.commit(FlyweightEvent.HEADER_LENGTH + length);
                }
                notifySent();
                return SendingResult.SENT;
            } finally {
                context = null;
            }
        }

        private void skipCommand(final MutableDirectBuffer buffer, final int length) {
            FlyweightCommand.writeHeader(
                    FlyweightEvent.sourceId(buffer),
                    FlyweightEvent.sourceSequence(buffer),
                    FlyweightEvent.eventTime(buffer),
                    FlyweightEvent.payloadType(buffer),
                    length,
                    buffer, CommandDescriptor.HEADER_OFFSET
            );
            skippedCommand.wrapSilently(buffer, CommandDescriptor.HEADER_OFFSET, buffer, EventDescriptor.PAYLOAD_OFFSET);
            try {
                duplicateHandler.skipCommandProcessing(skippedCommand);
            } catch (final Throwable t) {
                exceptionHandler.handleCommandProcessorException(skippedCommand, t);
            } finally {
                skippedCommand.reset();
            }
        }

        private void applyEvent(final DirectBuffer buffer, final int length) {
            if (passthroughApplierOrNull != null) {
                passthroughApplierOrNull.onEvent(sourceId(), sourceSequence(), eventSequence(), INDEX_0);
                return;
            }
            appliedEvent.wrapSilently(buffer, EventDescriptor.HEADER_OFFSET);
            try {
                eventApplier.onEvent(appliedEvent);
            } catch (final Throwable t) {
                exceptionHandler.handleEventApplierException(appliedEvent, t);
            } finally {
                appliedEvent.reset();
            }
        }

        @Override
        public void abort() {
            if (context != null) {
                buffer.unwrap();
                try {
                    context.abort();
                } finally {
                    context = null;
                }
            }
        }

        @Override
        public boolean isClosed() {
            return context == null;
        }
    }
}
