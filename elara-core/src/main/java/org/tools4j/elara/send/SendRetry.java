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

import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import static java.util.Objects.requireNonNull;

public final class SendRetry {

    private final DirectBuffer buffer = new UnsafeBuffer(0, 0);

    private RetrySupport retrySupport = UNINITIALISED;
    private long sequence;

    public SendingResult sendRetry() {
        return retrySupport.sendRetry(buffer, sequence, this);
    }

    public void init(final RetrySupport retrySupport) {
        this.retrySupport = requireNonNull(retrySupport);
        this.sequence = retrySupport.setup(buffer);
    }

    public void reset() {
        init(UNINITIALISED);
    }

    public interface RetrySupport {
        long setup(DirectBuffer buffer);
        SendingResult sendRetry(DirectBuffer buffer, long sequence, SendRetry sendRetry);
    }

    public interface RetryNotPossible extends RetrySupport {
        @Override
        default long setup(final DirectBuffer buffer) {
            buffer.wrap(0, 0);
            return 0;
        }
    }

    public static RetryNotPossible UNINITIALISED = (buf, seq, retry) -> {
        throw new IllegalStateException("Cannot send retry without failed send attempt");
    };

    public static RetryNotPossible SENT = (buf, seq, retry) -> {
        throw new IllegalStateException("Cannot send retry after successful send");
    };

    public static RetryNotPossible CLOSED = (buf, seq, retry) -> SendingResult.CLOSED;
}
