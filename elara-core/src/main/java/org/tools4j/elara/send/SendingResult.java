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
package org.tools4j.elara.send;

/**
 * Result returned by the send methods of {@link CommandSender} or
 * {@link org.tools4j.elara.send.CommandSender.SendingContext SendingContext} with information about the outcome of the
 * sending operation and with resend options in case failure.
 */
public interface SendingResult {
    /** @return the sending {@link Status} */
    Status status();
    /** @return true if sending was successful */
    boolean isSent();
    /** @return true if sending is pending after a timed resend attempt */
    boolean isPending();
    /** @return true if sending is neither successful nor pending */
    boolean isUnsuccessful();
    /** @return exception that occurred when attempting to send, or null if no exception occurred */
    Exception exception();
    /**
     * Immediately attempts to send the command again after a failed attempt.
     * @return this result with the updated sending result status
     * @throws IllegalStateException if this result does not reflect a failed attempt when retrying
     */
    SendingResult sendAgainImmediately();
    /**
     * Attempts to send the command again after a failed attempt waiting the specified {@code time} before resending.
     *
     * @param time the time after which to re-attempt sending of the command
     * @return this result with the updated sending result status
     * @throws IllegalStateException if this result does not reflect a failed attempt when retrying
     */
    SendingResult sendAgainAfter(long time);

    /** Status after sending a command */
    enum Status {
        /** Sending was successful */
        SENT,
        /** Resending was scheduled with a delay and is still pending */
        PENDING,
        /** Sending failed because the sender is not currently connected to the receiver */
        DISCONNECTED,
        /** Sending failed because the receiver experiences backpressure */
        BACK_PRESSURED,
        /** Sending failed with an {@link #exception() exception} */
        FAILED
    }

    /** Interface extension providing default implementations */
    interface Default extends SendingResult {
        @Override
        default boolean isSent() {
            return status() == Status.SENT;
        }

        @Override
        default boolean isPending() {
            return status() == Status.PENDING;
        }

        @Override
        default boolean isUnsuccessful() {
            final Status status = status();
            return status != Status.SENT && status != Status.PENDING;
        }
    }

    SendingResult SENT = new Default() {
        @Override
        public Status status() {
            return Status.SENT;
        }

        @Override
        public Exception exception() {
            return null;
        }

        @Override
        public SendingResult sendAgainImmediately() {
            throw new IllegalStateException("Cannot send again after command was successfully sent");
        }

        @Override
        public SendingResult sendAgainAfter(final long time) {
            throw new IllegalStateException("Cannot send again after command was successfully sent");
        }
    };
}
