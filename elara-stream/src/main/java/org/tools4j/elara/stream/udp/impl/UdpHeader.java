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

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;

final class UdpHeader implements MutableNioHeader {
    public final static int HEADER_LENGTH = 16;
    private static final int POS_PAYLOAD_LENGTH = 0;
    private static final int POS_SEQUENCE = 8;

    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    @Override
    public int headerLength() {
        return HEADER_LENGTH;
    }

    @Override
    public MutableDirectBuffer buffer() {
        return buffer;
    }

    @Override
    public int payloadLength() {
        return buffer.getInt(POS_PAYLOAD_LENGTH);
    }

    public long sequence() {
        return buffer.getLong(POS_SEQUENCE);
    }

    @Override
    public void payloadLength(final int length) {
        buffer.putInt(POS_PAYLOAD_LENGTH, length);
    }

    public void sequence(final long sequence) {
        buffer.putLong(POS_SEQUENCE, sequence);
    }
}
