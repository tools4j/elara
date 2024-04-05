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
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.flyweight.PayloadType;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.stream.SendingResult;

/**
 * Provides functions to encode and send commands when polling an {@link Input input} and {@link SingleSourceInput}.
 * <p>
 * Command sending can be done in two ways: already coded commands can be sent via one of the
 * {@link #sendCommand(DirectBuffer, int, int) sendCommand(..)} methods.  Alternatively the command can be encoded into
 * the sending transport buffer directly as follows:
 * <pre>
 *      try (SendingContext context = sendingEvent()) {
 *          int length = 0;
 *          length += context.buffer().putInt(0, 123);
 *          length += context.buffer().putStringAscii(4, "Hello world");
 *          context.send(length);
 *     }
 * </pre>
 * Note that {@code SendingContext} implements {@link AutoCloseable} and if command sending is performed inside a
 * try-resource block as in the example above then sending will be {@link SendingContext#abort() aborted} automatically
 * if {@link SendingContext#send(int) send(..)} is not called for instance due to an exception.
 */
public interface CommandSender {
    /**
     * Starts sending of a command and returns the sending context with the buffer for command encoding.  Encoding and
     * sending are completed with {@link SendingContext#send(int) send(..)} and it is recommended to perform the
     * encoding inside a try-resource block; see {@link CommandSender class documentation} for an example.
     * <p>
     * The command uses the {@link PayloadType#DEFAULT DEFAULT} payload type.
     *
     * @return the context for command encoding and sending
     * @see #sendingCommand(int)
     */
    SendingContext sendingCommand();
    
    /**
     * Starts sending of a command and returns the sending context with the buffer for command encoding.  Encoding and
     * sending are completed with {@link SendingContext#send(int) send(..)} and it is recommended to perform the
     * encoding inside a try-resource block; see {@link CommandSender class documentation} for an example.
     *
     * @param payloadType the payload type, typically non-negative for application commands (plugins use negative types)
     * @return the context for command encoding and sending
     */
    SendingContext sendingCommand(int payloadType);

    /***
     * Sends a command that has already encoded into the given buffer.
     * <p>
     * The command uses the {@link PayloadType#DEFAULT DEFAULT} payload type.
     *
     * @param buffer    the buffer containing the command data
     * @param offset    offset where the command data starts in {@code buffer}
     * @param length    the length of the command data in bytes
     * @return the result indicating whether sending was successful, with options to resend after failures
     * @see #sendCommand(int, DirectBuffer, int, int)
     */
    SendingResult sendCommand(DirectBuffer buffer, int offset, int length);

    /***
     * Sends a command that has already encoded into the given buffer.
     *
     * @param payloadType   the payload type, typically non-negative for application commands (plugins use negative types)
     * @param buffer        the buffer containing the command data
     * @param offset        offset where the command data starts in {@code buffer}
     * @param length        the length of the command data in bytes
     * @return the result indicating whether sending was successful, with options to resend after failures
     */
    SendingResult sendCommand(int payloadType, DirectBuffer buffer, int offset, int length);

    /***
     * Sends a command that has a zero-length payload and the specified payload type.
     *
     * @param payloadType the payload type, typically non-negative for application commands (plugins use negative types)
     * @return the result indicating whether sending was successful, with options to resend after failures
     */
    SendingResult sendCommandWithoutPayload(int payloadType);

    /**
     * Returns the source ID associated with the node sending commands with this command sender.
     * @return the source ID used for commands sent by this command sender
     */
    int sourceId();

    /**
     * Returns the sequence of the next command to be sent.  If sending has started via {@link #sendingCommand()}
     * then the sequence refers to the command currently being encoded.
     *
     * @return sequence of the next command to be sent.
     */
    long nextCommandSequence();

    /**
     * Context object returned by {@link #sendingCommand()} allowing for zero copy encoding of commands directly into
     * the sending transport buffer.  Sending contexts are typically used inside a try-resource block; see
     * {@code CommandSender} {@link CommandSender documentation} for usage example.
     */
    interface SendingContext extends AutoCloseable {
        /** @return source ID of the command currently being encoded and about to be sent */
        int sourceId();
        /** @return sequence of the command currently being encoded and about to be sent */
        long sourceSequence();

        /**
         * Returns the buffer to encode the command directly into the sending transport buffer.
         *
         * @return the buffer for coding of command data directly into the sending transport buffer
         *
         * @throws IllegalStateException if this sending context has already been {@link #isClosed() closed}
         */
        MutableDirectBuffer buffer();

        /**
         * Completes command encoding and sends the command.
         *
         * @param length the encoding length for the command to be sent
         * @throws IllegalArgumentException if length is negative
         * @throws IllegalStateException if this sending context has already been {@link #isClosed() closed}
         * @return the result indicating whether sending was successful, with options to resend after failures
         */
        SendingResult send(int length);

        /**
         * Aborts sending of the command -- identical to {@link #close()}; ignored if the sending context is already
         * {@link #isClosed() closed}.
         */
        void abort();

        /**
         * Returns true if this sending context has already been closed through either of {@link #send(int)},
         * {@link #abort()} or {@link #close()}.
         *
         * @return true if this sending context is closed (command sent or sending aborted)
         */
        boolean isClosed();

        /**
         * Aborts sending of the command -- identical to {@link #abort()}; ignored if the sending context is already
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
     * Provides default methods for {@link CommandSender}.
     */
    interface Default extends CommandSender {
        @Override
        default SendingContext sendingCommand() {
            return sendingCommand(PayloadType.DEFAULT);
        }

        @Override
        default SendingResult sendCommand(final DirectBuffer buffer, final int offset, final int length) {
            return sendCommand(PayloadType.DEFAULT, buffer, offset, length);
        }

        @Override
        default SendingResult sendCommand(final int payloadType, final DirectBuffer buffer, final int offset, final int length) {
            try (final SendingContext context = sendingCommand(payloadType)) {
                context.buffer().putBytes(0, buffer, offset, length);
                return context.send(length);
            }
        }

        @Override
        default SendingResult sendCommandWithoutPayload(final int payloadType) {
            try (final SendingContext context = sendingCommand(payloadType)) {
                return context.send(0);
            }
        }
    }
}
