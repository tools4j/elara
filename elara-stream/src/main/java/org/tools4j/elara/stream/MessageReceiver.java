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
package org.tools4j.elara.stream;

import org.agrona.DirectBuffer;

/**
 * Facilitates receiving of messages on arbitrary transport media.
 * <p>
 * Messages are received by calling the {@link #poll(Handler) poll(..)} method with a call-back handler that is invoked
 * if a message is available.
 */
public interface MessageReceiver extends MessageStream {
    /**
     * Poll method invoked with a call-back handler to receive the message if available.
     *
     * @param handler the message handler called back with messages if any are available
     * @return the number indicating the number of messages that were received
     */
    int poll(Handler handler);

    /**
     * Call-back handler for messages received when invoking the {@link MessageReceiver#poll(Handler) poll(..)} method.
     */
    interface Handler {
        /**
         * Call-back message invoked with messages received when invoking the
         * {@link MessageReceiver#poll(Handler) poll(..)} method.
         *
         * @param message the message received, with message bytes from position {@code [0-(length-1)}, where length
         *                equals the buffer's {@link DirectBuffer#capacity() capacity}.
         */
        void onMessage(DirectBuffer message);
    }

    /** No-op constant for a message receiver that has been closed */
    MessageReceiver CLOSED = new ClosedMessageReceiver();
}
