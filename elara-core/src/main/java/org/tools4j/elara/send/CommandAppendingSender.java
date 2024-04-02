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
package org.tools4j.elara.send;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.store.ExpandableDirectBuffer;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.CommandDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.CommandDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.CommandDescriptor.PAYLOAD_OFFSET;

/**
 * A command sender that appends the command to a command store.
 */
public final class CommandAppendingSender extends FlyweightCommandSender {

    private final TimeSource timeSource;
    private final MessageStore.Appender commandStoreAppender;
    private final SendingContext sendingContext = new SendingContext();

    public CommandAppendingSender(final TimeSource timeSource, final MessageStore.Appender commandStoreAppender) {
        this.timeSource = requireNonNull(timeSource);
        this.commandStoreAppender = requireNonNull(commandStoreAppender);
    }

    @Override
    public CommandSender.SendingContext sendingCommand(final int payloadType) {
        return sendingContext.init(sourceId(), nextCommandSequence(), payloadType, commandStoreAppender.appending());
    }

    private final class SendingContext implements CommandSender.SendingContext {

        final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        AppendingContext context;

        SendingContext init(final int sourceId, final long sequence, final int payloadType, final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightCommand.writeHeader(
                    sourceId, sequence, TimeSource.MIN_VALUE, payloadType, 0,
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
        public int sourceId() {
            return FlyweightCommand.sourceId(unclosedContext().buffer());
        }

        @Override
        public long sourceSequence() {
            return FlyweightCommand.sourceSequence(unclosedContext().buffer());
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
                final MutableDirectBuffer buf = ac.buffer();
                if (length > 0) {
                    FlyweightCommand.writePayloadSize(length, buf);
                }
                final long time = timeSource.currentTime();
                FlyweightCommand.writeCommandTime(time, buf);
                ac.commit(HEADER_LENGTH + length);
                notifySent(time);
                return SendingResult.SENT;
            } finally {
                context = null;
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
