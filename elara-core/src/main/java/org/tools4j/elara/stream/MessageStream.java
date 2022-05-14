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
import org.tools4j.elara.stream.MessageStream.Handler.Result;

/**
 * A stream of messages;  messages can be {@link #appender() appended} and {@link #poller() polled}.  A message stream
 * can be a persisted {@link org.tools4j.elara.store.MessageStore} or it can be an input or output stream of messages.
 */
public interface MessageStream extends AutoCloseable {
    Appender appender();
    Poller poller();
    Poller poller(String id);
    @Override
    void close();

    interface Appender extends AutoCloseable {
        default void append(DirectBuffer buffer, int offset, int length) {
            try (final AppendingContext ctxt = appending()) {
                ctxt.buffer().putBytes(0, buffer, offset, length);
                ctxt.commit(length);
            }
        }
        AppendingContext appending();

        @Override
        void close();
    }

    interface AppendingContext extends AutoCloseable {
        MutableDirectBuffer buffer();

        void commit(int length);
        void abort();
        boolean isClosed();

        @Override
        default void close() {
            if (!isClosed()) {
                abort();
            }
        }
    }

    interface Poller extends AutoCloseable {
        int poll(Handler handler);

        default Poller moveToEnd() {
            //noinspection StatementWithEmptyBody
            while (0 < poll(message -> Result.POLL));
            return this;
        }

        @Override
        void close();
    }

    @FunctionalInterface
    interface Handler {
        enum Result {
            /** Marks message as peeked so that it will be revisited when polling again */
            PEEK,
            /** Marks message as consumed so that the next message is returned when polling again */
            POLL
        }
        Result onMessage(DirectBuffer message);
    }
}
