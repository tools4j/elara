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

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;

import static java.util.Objects.requireNonNull;

final class NioFrameImpl implements NioFrame {
    private final MutableNioHeader header;
    private final MutableDirectBuffer frame;
    private final MutableDirectBuffer payload;

    NioFrameImpl(final MutableNioHeader header, final int initialCapacity) {
        this(header, new ExpandableDirectByteBuffer(initialCapacity));
    }

    NioFrameImpl(final MutableNioHeader header, final MutableDirectBuffer frame) {
        this.frame = requireNonNull(frame);
        this.header = requireNonNull(header);
        this.payload = new UnsafeBuffer(frame, header.headerLength(), frame.capacity() - header.headerLength());
        header.buffer().wrap(frame, 0, header.headerLength());
    }

    @Override
    public MutableNioHeader header() {
        return header;
    }

    @Override
    public MutableDirectBuffer frame() {
        return frame;
    }

    @Override
    public MutableDirectBuffer payload() {
        return payload;
    }
}
