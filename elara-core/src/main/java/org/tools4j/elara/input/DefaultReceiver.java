/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.input;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.FlyweightHeader;
import org.tools4j.elara.log.ExpandableDirectBuffer;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.MessageLog.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;

public final class DefaultReceiver implements Receiver.Default {

    private final TimeSource timeSource;
    private final MessageLog.Appender commandLogAppender;
    private final ReceivingContext receivingContext = new ReceivingContext();

    public DefaultReceiver(final TimeSource timeSource, final MessageLog.Appender commandLogAppender) {
        this.timeSource = requireNonNull(timeSource);
        this.commandLogAppender = requireNonNull(commandLogAppender);
    }

    @Override
    public ReceivingContext receivingMessage(final int source, final long sequence, final int type) {
        return receivingContext.init(source, sequence, type, commandLogAppender.appending());
    }

    private final class ReceivingContext implements Receiver.ReceivingContext {

        final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        AppendingContext context;

        ReceivingContext init(final int source, final long sequence, final int type, final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Receiving context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightHeader.writeTo(
                    source, type, sequence, timeSource.currentTime(), Flags.NONE, FlyweightCommand.INDEX, 0,
                    context.buffer(), HEADER_OFFSET
            );
            return this;
        }

        AppendingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Receiving context is closed");
        }

        @Override
        public MutableDirectBuffer buffer() {
            unclosedContext();
            return buffer;
        }

        @Override
        public void receive(final int length) {
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            buffer.unwrap();
            try (final AppendingContext ac = unclosedContext()) {
                if (length > 0) {
                    ac.buffer().putInt(PAYLOAD_SIZE_OFFSET, length);
                }
                ac.commit(HEADER_LENGTH + length);
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
