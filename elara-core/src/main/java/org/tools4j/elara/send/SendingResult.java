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

    /**
     * Immediately attempts to send the command again after a failed attempt.
     * @return this result with the updated sending result status
     * @throws IllegalStateException if this result does not reflect a failed attempt when retrying
     */
    SendingResult sendRetry();

    /** Status after sending a command */
    enum Status {
        /** Sending was successful */
        SENT,
        /** Sending failed because the sender is not currently connected to the receiver */
        DISCONNECTED,
        /** Sending failed because the receiver experiences backpressure */
        BACK_PRESSURED,
        /** Sending failed for another reason such as an exception */
        FAILED
    }

    /** Interface extension providing default implementations */
    interface Default extends SendingResult {
        @Override
        default boolean isSent() {
            return status() == Status.SENT;
        }
    }

    SendingResult SENT = new SendingResult() {
        @Override
        public Status status() {
            return Status.SENT;
        }

        @Override
        public boolean isSent() {
            return true;
        }

        @Override
        public SendingResult sendRetry() {
            throw new IllegalStateException("Cannot send again when already sent successfully");
        }
    };

    //FIXME remove below
    SendingResult DISCONNECTED = new Default() {
        @Override
        public Status status() {
            return Status.DISCONNECTED;
        }

        @Override
        public SendingResult sendRetry() {
            return this;
        }
    };

    //FIXME remove below
    SendingResult FAILED = new Default() {
        @Override
        public Status status() {
            return Status.FAILED;
        }

        @Override
        public SendingResult sendRetry() {
            throw new IllegalStateException("not implemented");//FIXME impl
        }
    };
}
