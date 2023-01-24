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
package org.tools4j.elara.plugin.replication;

import org.agrona.DirectBuffer;

import static java.util.Objects.requireNonNull;

public interface Connection {
    Poller poller();
    Publisher publisher();

    static Connection create(final Poller poller, final Publisher publisher) {
        requireNonNull(poller);
        requireNonNull(publisher);
        return new Connection() {
            @Override
            public Poller poller() {
                return poller;
            }

            @Override
            public Publisher publisher() {
                return publisher;
            }
        };
    }

    @FunctionalInterface
    interface Poller {
        int poll(Handler handler);
    }

    @FunctionalInterface
    interface Handler {
        void onMessage(int senderServerId, DirectBuffer buffer, int offset, int length);
    }

    @FunctionalInterface
    interface Publisher {
        boolean publish(int targetServerId, DirectBuffer buffer, int offset, int length);
    }
}
