/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.input;

import org.agrona.DirectBuffer;

import static java.util.Objects.requireNonNull;

/**
 * Tracks input messages and drops commands whose sequence number is not greater than the
 * sequence number of the last seen message.
 */
public class ExactlyOnceInput implements Input {

    private final Input input;
    private final Handler duplicateHandler;
    private long maxSequence;

    public ExactlyOnceInput(final Input input, final Handler duplicateHandler, final long initialSequence) {
        this.input = requireNonNull(input);
        this.duplicateHandler = requireNonNull(duplicateHandler);
        this.maxSequence = initialSequence;
    }

    public long maxSequence() {
        return maxSequence;
    }

    @Override
    public int id() {
        return input.id();
    }

    @Override
    public Poller poller() {
        return new TrackingPoller();
    }

    private final class TrackingPoller implements Poller {

        private final Poller poller = input.poller();
        private final Handler conditionalHandler = this::onMessage;

        private Handler handler;

        @Override
        public int poll(final Handler handler) {
            requireNonNull(handler);
            if (this.handler != handler) {
                this.handler = handler;
            }
            return poller.poll(conditionalHandler);
        }

        private void onMessage(final long sequence, final int type,
                               final DirectBuffer buffer, final int offset, final int length) {
            assert handler != null;
            if (sequence > maxSequence) {
                maxSequence = sequence;
                handler.onMessage(sequence, type, buffer, offset, length);
            } else {
                duplicateHandler.onMessage(sequence, type, buffer, offset, length);
            }
        }
    }
}
