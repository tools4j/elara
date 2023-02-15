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
package org.tools4j.elara.stream.tcp.impl;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.nio.NioHeader;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;

import java.nio.ByteBuffer;

final class TcpHeader implements MutableNioHeader, NioHeader {
    public static final int HEADER_LENGTH = 4;
    public static final int PAYLOAD_LENGTH_OFFSET = 0;
    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    @Override
    public int headerLength() {
        return HEADER_LENGTH;
    }

    @Override
    public int payloadLength() {
        return buffer.getInt(PAYLOAD_LENGTH_OFFSET);
    }

    @Override
    public void payloadLength(final int length) {
        buffer.putInt(PAYLOAD_LENGTH_OFFSET, length);
    }

    @Override
    public boolean valid() {
        return buffer.capacity() > 0;
    }

    @Override
    public void wrap(final ByteBuffer source, final int offset) {
        buffer.wrap(source, offset, HEADER_LENGTH);
    }

    @Override
    public void wrap(final DirectBuffer source, final int offset) {
        buffer.wrap(source, offset, HEADER_LENGTH);
    }

    @Override
    public void unwrap() {
        buffer.wrap(0, 0);
    }
}
