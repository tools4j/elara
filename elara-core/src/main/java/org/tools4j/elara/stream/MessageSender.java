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
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.send.SendingResult;

/**
 * Facilitates sending of messages on arbitrary transport media.
 * <p>
 * Message sending can be done in two ways: messages already stored in a buffer can be sent via one of the
 * {@link #sendMessage(DirectBuffer, int, int) sendMessage(..)} methods.  Alternatively the message can be encoded into
 * the sending transport buffer directly as follows:
 * <pre>
 *     try (SendingContext context = sendingMessage()) {
 *         int length = context.buffer().putStringAscii(0, "Hello world");
 *         context.send(length);
 *     }
 * </pre>
 * Note that {@code SendingContext} implements {@link AutoCloseable} and if message sending is performed inside a
 * try-resource block as in the example above then sending will be {@link SendingContext#abort() aborted} automatically
 * if {@link SendingContext#send(int) send(..)} is not called for instance due to an exception.
 */
public interface MessageSender extends MessageStream {
    /***
     * Sends the message already encoded in the given buffer.
     *
     * @param buffer    the buffer containing the message data
     * @param offset    offset where the message data starts in {@code buffer}
     * @param length    the length of the message data in bytes
     * @return the result indicating whether sending was successful, with options to resend after failures
     */
    SendingResult sendMessage(DirectBuffer buffer, int offset, int length);

    /**
     * Starts sending of a message and returns the sending context with the buffer for to encode the message directly
     * into the transport buffer.  Encoding and sending is completed with {@link SendingContext#send(int) send(..)}
     * and is recommended to be performed inside a try-resource block; see {@link MessageSender class documentation} for
     * an example.
     *
     * @return the context for message encoding and sending
     */
    SendingContext sendingMessage();

    /**
     * Context object returned by {@link #sendingMessage()} allowing for zero copy encoding of messages directly into
     * the sending transport buffer.  Sending contexts are typically used inside a try-resource block; see
     * {@code MessageSender} {@link MessageSender documentation} for usage example.
     */
    interface SendingContext extends AutoCloseable {
        /**
         * Returns the buffer to encode the message directly into the sending transport buffer.
         *
         * @return the buffer for coding of message data directly into the sending transport buffer
         *
         * @throws IllegalStateException if this sending context has already been {@link #isClosed() closed}
         */
        MutableDirectBuffer buffer();

        /**
         * Completes message encoding and sends the message.
         *
         * @param length the encoding length for the message to be sent
         * @throws IllegalArgumentException if length is negative
         * @throws IllegalStateException if this sending context has already been {@link #isClosed() closed}
         * @return the result indicating whether sending was successful, with options to resend after failures
         */
        SendingResult send(int length);

        /**
         * Aborts sending of the message -- identical to {@link #close()}; ignored if the sending context is already
         * {@link #isClosed() closed}.
         */
        void abort();

        /**
         * Returns true if this sending context has already been closed through either of {@link #send(int)},
         * {@link #abort()} or {@link #close()}.
         *
         * @return true if this sending context is closed (message sent or sending aborted)
         */
        boolean isClosed();

        /**
         * Aborts sending of the message -- identical to {@link #abort()}; ignored if the sending context is already
         * {@link #isClosed() closed}.
         */
        @Override
        default void close() {
            if (!isClosed()) {
                abort();
            }
        }
    }

    /**
     * Provides default implementation for transports with support for encoding directly into the transport buffer.
     */
    interface Direct extends MessageSender {
        @Override
        default SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
            try (final SendingContext context = sendingMessage()) {
                context.buffer().putBytes(0, buffer, offset, length);
                return context.send(length);
            }
        }
    }

    /**
     * Provides default implementation for transports without direct transport buffer support;  messages are encoded
     * into a reusable buffer if necessary.
     */
    abstract class Buffered implements MessageSender {
        private final BufferingSendingContext context;

        /**
         * Constructor with initial buffer size
         * @param initialBufferSize the initial capacity of the buffer used to code messages to
         */
        public Buffered(final int initialBufferSize) {
            this(new ExpandableDirectByteBuffer(initialBufferSize));
        }

        /**
         * Constructor with buffer used for messages encoded with this sender
         * @param buffer the buffer used to code messages to
         */
        public Buffered(final MutableDirectBuffer buffer) {
            this.context = new BufferingSendingContext(this, buffer);
        }

        @Override
        public SendingContext sendingMessage() {
            return context.init();
        }
    }

    /** No-op constant for a message sender that has been closed */
    MessageSender CLOSED = new ClosedMessageSender();
}
