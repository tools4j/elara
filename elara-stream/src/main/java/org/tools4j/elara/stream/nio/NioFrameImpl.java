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
package org.tools4j.elara.stream.nio;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;

import static java.util.Objects.requireNonNull;

final class NioFrameImpl implements NioFrame {
    private final MutableDirectBuffer buffer;
    private final MutableNioHeader header;
    private final MutableDirectBuffer payload;
    private final DirectBuffer frame;

    NioFrameImpl(final MutableNioHeader header, final int initialCapacity) {
        this(header, new ExpandableDirectByteBuffer(initialCapacity));
    }

    NioFrameImpl(final MutableNioHeader header, final MutableDirectBuffer buffer) {
        this.buffer = requireNonNull(buffer);
        this.header = requireNonNull(header);
        this.payload = new UnsafeBuffer(buffer, header.headerLength(), buffer.capacity() - header.headerLength());
        this.frame = new UnsafeBuffer(buffer);
        header.wrap(buffer, 0);
    }

    @Override
    public MutableNioHeader header() {
        return header;
    }

    MutableDirectBuffer payload() {
        return payload;
    }

    @Override
    public DirectBuffer frame() {
        frame.wrap(buffer, 0, header.headerLength() + header.payloadLength());
        return frame;
    }
}
