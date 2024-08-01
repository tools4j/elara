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

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.CommandDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.CommandDescriptor.HEADER_OFFSET;

/**
 * A command sender that directly invokes the command handler without persisting the command.
 */
public final class CommandHandlingSender extends FlyweightCommandSender {

    private static final DirectBuffer EMPTY_BUFFER = new UnsafeBuffer(0, 0);

    private final TimeSource timeSource;
    private final CommandHandler commandHandler;
    private final SendingContext commandContext;
    private final MutableDirectBuffer header = new ExpandableDirectByteBuffer(HEADER_LENGTH);
    private final FlyweightCommand command = new FlyweightCommand();

    public CommandHandlingSender(final int initialBufferCapacity,
                                 final TimeSource timeSource,
                                 final CommandHandler commandHandler) {
        this.timeSource = requireNonNull(timeSource);
        this.commandHandler = requireNonNull(commandHandler);
        this.commandContext = new SendingContext(initialBufferCapacity);
    }

    @Override
    public CommandSender.SendingContext sendingCommand(final int payloadType) {
        return commandContext.init(sourceId(), nextCommandSequence(), payloadType);
    }

    @Override
    public SendingResult sendCommandWithoutPayload(final int payloadType) {
        return sendCommand(payloadType, EMPTY_BUFFER, 0, 0);
    }

    @Override
    public SendingResult sendCommand(final int payloadType, final DirectBuffer buffer, final int offset, final int length) {
        final long time = timeSource.currentTime();
        initHeader(sourceId(), nextCommandSequence(), time, payloadType, length);
        invokeCommandHandler(buffer, offset, length);//TODO handle result value here
        notifySent(time, length);
        return SendingResult.SENT;
    }

    private void initHeader(final int sourceId, final long sourceSeq, final long commandTime, final int payloadType, final int payloadSize) {
        FlyweightCommand.writeHeader(
                sourceId, sourceSeq, commandTime, payloadType, payloadSize, header, HEADER_OFFSET
        );
    }

    private void invokeCommandHandler(final DirectBuffer payload, final int offset, final int length) {
        command.wrapSilently(header, HEADER_OFFSET, payload, offset);
        assert length == command.payloadSize();
        try {
            commandHandler.onCommand(command);
        } finally {
            command.reset();
        }
    }

    private final class SendingContext implements CommandSender.SendingContext {

        final MutableDirectBuffer payload;
        boolean closed = true;

        SendingContext(final int initialBufferCapacity) {
            this.payload = new ExpandableDirectByteBuffer(initialBufferCapacity);
        }

        SendingContext init(final int sourceId, final long sourceSeq, final int type) {
            if (!closed) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            initHeader(sourceId, sourceSeq, TimeSource.MIN_VALUE, type, 0);
            closed = false;
            return this;
        }

        void ensureNotClosed() {
            if (closed) {
                throw new IllegalStateException("Sending context is closed");
            }
        }

        @Override
        public int sourceId() {
            ensureNotClosed();
            return FlyweightCommand.sourceId(header);
        }

        @Override
        public long sourceSequence() {
            ensureNotClosed();
            return FlyweightCommand.sourceSequence(header);
        }

        @Override
        public MutableDirectBuffer buffer() {
            ensureNotClosed();
            return payload;
        }

        @Override
        public SendingResult send(final int length) {
            ensureNotClosed();
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            try {
                if (length > 0) {
                    FlyweightCommand.writePayloadSize(length, header);
                }
                final long time = timeSource.currentTime();
                FlyweightCommand.writeCommandTime(time, header);
                invokeCommandHandler(payload, 0, length);//TODO handler result value here
                notifySent(time, length);
                return SendingResult.SENT;
            } finally {
                command.reset();
                closed = true;
            }
        }

        @Override
        public void abort() {
            if (!closed) {
                closed = true;
            }
        }

        @Override
        public boolean isClosed() {
            return closed;
        }
    }
}
