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
package org.tools4j.elara.send;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.flyweight.FlyweightHeader;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.base.EventIdApplier;
import org.tools4j.elara.store.ExpandableDirectBuffer;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.INDEX_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.SEQUENCE_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.SOURCE_OFFSET;

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
    private final EventIdApplier eventIdApplierOrNull;
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
        this.eventIdApplierOrNull = eventApplier instanceof EventIdApplier ? (EventIdApplier)eventApplier : null;
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.duplicateHandler = requireNonNull(duplicateHandler);
    }

    @Override
    public CommandSender.SendingContext sendingCommand(final int type) {
        return sendingContext.init(source(), nextCommandSequence(), type, eventStoreAppender.appending());
    }

    private final class SendingContext implements CommandSender.SendingContext {

        private static final short INDEX = 0;
        final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        AppendingContext context;

        SendingContext init(final int source, final long sequence, final int type, final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightHeader.writeTo(
                    source, type, sequence, timeSource.currentTime(), Flags.NONE, INDEX, 0,
                    context.buffer(), HEADER_OFFSET
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
        public int source() {
            return unclosedContext().buffer().getInt(SOURCE_OFFSET);
        }

        @Override
        public long sequence() {
            return unclosedContext().buffer().getLong(SEQUENCE_OFFSET);
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
                    ac.buffer().putInt(PAYLOAD_SIZE_OFFSET, length);
                }
                if (baseState.allEventsAppliedFor(source(), sequence())) {
                    skipCommand(ac.buffer(), length);
                } else {
                    applyEvent(ac.buffer(), length);
                    ac.commit(HEADER_LENGTH + length);
                }
                incrementCommandSequence();
                return SendingResult.SENT;
            } finally {
                context = null;
            }
        }

        private void skipCommand(final MutableDirectBuffer buffer, final int length) {
            buffer.putByte(FLAGS_OFFSET, Flags.NONE);
            buffer.putShort(INDEX_OFFSET, FlyweightCommand.INDEX);
            skippedCommand.initSilent(buffer, HEADER_OFFSET, buffer, PAYLOAD_OFFSET, length);
            try {
                duplicateHandler.skipCommandProcessing(skippedCommand);
            } catch (final Throwable t) {
                exceptionHandler.handleCommandProcessorException(skippedCommand, t);
            } finally {
                skippedCommand.reset();
            }
        }

        private void applyEvent(final DirectBuffer buffer, final int length) {
            if (eventIdApplierOrNull != null) {
                eventIdApplierOrNull.onEventId(source(), sequence(), INDEX);
                return;
            }
            appliedEvent.initSilent(buffer, HEADER_OFFSET, buffer, PAYLOAD_OFFSET, length);
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
