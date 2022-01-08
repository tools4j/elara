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
package org.tools4j.elara.log;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

public interface MessageLog extends AutoCloseable {
    Appender appender();
    Poller poller();
    Poller poller(String id);
    @Override
    void close();

    @FunctionalInterface
    interface Appender {
        default void append(DirectBuffer buffer, int offset, int length) {
            try (final AppendingContext ctxt = appending()) {
                ctxt.buffer().putBytes(0, buffer, offset, length);
                ctxt.commit(length);
            }
        }
        AppendingContext appending();
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

    interface Poller {
        /**
         * Returns the current entry ID of this poller.
         * <p>
         * It returns the entry ID of the message currently polled if invoked while polling a message.
         * Otherwise, it returns the entry ID of the next message to be polled.
         * <p>
         * The index is monotonically increasing but not necessarily gap free;  for instance for a chronicle message log
         * the entry ID consists of cycle and the sequence number within that cycle.
         *
         * @return the current entry ID of this poller.
         *
         */
        long entryId();
        boolean moveTo(long entryId);
        boolean moveToNext();//skip one
        boolean moveToPrevious();//skip one
        Poller moveToStart();
        Poller moveToEnd();
        int poll(Handler handler);
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
