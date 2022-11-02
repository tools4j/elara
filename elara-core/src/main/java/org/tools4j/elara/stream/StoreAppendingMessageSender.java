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
package org.tools4j.elara.stream;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.AppendingContext;

import static java.util.Objects.requireNonNull;

public class StoreAppendingMessageSender implements MessageSender {

    private final MessageStore.Appender messageStoreAppender;
    private final SendingContext sendingContext = new SendingContext();

    public StoreAppendingMessageSender(final MessageStore messageStore) {
        this(messageStore.appender());
    }

    public StoreAppendingMessageSender(final MessageStore.Appender messageStoreAppender) {
        this.messageStoreAppender = requireNonNull(messageStoreAppender);
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        messageStoreAppender.append(buffer, offset, length);
        return SendingResult.SENT;
    }

    @Override
    public MessageSender.SendingContext sendingMessage() {
        return sendingContext.init(messageStoreAppender.appending());
    }

    @Override
    public boolean isClosed() {
        return messageStoreAppender.isClosed();
    }

    @Override
    public void close() {
        sendingContext.close();
        messageStoreAppender.close();
    }

    @Override
    public String toString() {
        return "StoreAppendingMessageSender";
    }

    private static final class SendingContext implements MessageSender.SendingContext {

        AppendingContext context;

        SendingContext init(final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Sending context not closed");
            }
            this.context = requireNonNull(context);
            return this;
        }

        AppendingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Sending context is closed");
        }

        @Override
        public MutableDirectBuffer buffer() {
            return unclosedContext().buffer();
        }

        @Override
        public SendingResult send(final int length) {
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            try (final AppendingContext ac = unclosedContext()) {
                ac.commit(length);
                return SendingResult.SENT;
            } finally {
                context = null;
            }
        }

        @Override
        public void abort() {
            if (context != null) {
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
