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
package org.tools4j.elara.stream.udp.impl;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.stream.nio.NioHeader;

final class UdpHeader implements NioHeader {
    final static int LENGTH = 16;
    final static int POS_PAYLOAD_LENGTH = 0;
    final static int POS_SEQUENCE = 8;

    private long sequence;

    @Override
    public int headerLength() {
        return LENGTH;
    }

    @Override
    public void write(final MutableDirectBuffer header, final DirectBuffer payload, final int payloadLength) {
        header.putInt(POS_PAYLOAD_LENGTH, payloadLength);
        header.putLong(POS_SEQUENCE, sequence);
    }

    int outOfSequenceCount;

    @Override
    public int payloadLength(final DirectBuffer frame) {
        final int payloadLength = frame.getInt(POS_PAYLOAD_LENGTH);
        final long sequence = frame.getLong(POS_SEQUENCE);
        if (this.sequence != sequence) {
            System.err.println("Unexpected sequence " + sequence + " when expecting " + this.sequence);
            outOfSequenceCount++;
            if (outOfSequenceCount > 10) {
                throw new IllegalArgumentException("Unexpected sequence " + sequence + " when expecting " + this.sequence);
            }
        }
        this.sequence = sequence + 1;
        return payloadLength;
    }

    void incrementSequence() {
        sequence++;
    }
}
