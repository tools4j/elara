/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.samples.network;

import org.agrona.DirectBuffer;

import static java.util.Objects.requireNonNull;

public class DefaultServerTopology implements ServerTopology {

    private final Buffer[] sendBuffers;
    private final Buffer[][] receiveBuffers;
    private final Transmitter transmitter;

    public DefaultServerTopology(final Buffer[] sendBuffers,
                                 final Buffer[][] receiveBuffers,
                                 final Transmitter transmitter) {
        for (int i = 0; i < receiveBuffers.length; i++) {
            if (receiveBuffers[i].length != sendBuffers.length) {
                throw new IllegalArgumentException(
                        "receiveBuffers[" + i + "].length must be same as sendBuffers.length, but " +
                        receiveBuffers[i].length + " != " + sendBuffers.length
                );
            }
        }
        this.sendBuffers = requireNonNull(sendBuffers);
        this.receiveBuffers = requireNonNull(receiveBuffers);
        this.transmitter = requireNonNull(transmitter);
    }

    @Override
    public int senders() {
        return sendBuffers.length;
    }

    @Override
    public int receivers() {
        return receiveBuffers.length;
    }

    @Override
    public boolean transmit(final int sender, final int receiver, final DirectBuffer buffer, final int offset, final int length) {
        final Buffer senderBuffer = sendBuffers[sender];
        if (!senderBuffer.offer(buffer, offset, length)) {
            return false;
        }
        final Buffer receiverBuffer = receiveBuffers[receiver][sender];
        transmitter.transmit(senderBuffer, receiverBuffer);
        return true;
    }

    @Override
    public Buffer[] receiveBuffers(final int receiver) {
        return receiveBuffers[receiver];
    }

    @Override
    public Buffer receiveBuffer(final int sender, final int receiver) {
        return receiveBuffers[receiver][sender];
    }
}
