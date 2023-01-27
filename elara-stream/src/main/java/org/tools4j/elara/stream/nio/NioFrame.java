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

import static java.util.Objects.requireNonNull;

final class NioFrame {
    private final int headerLength;
    private final MutableDirectBuffer buffer;
    private final MutableDirectBuffer header;
    private final MutableDirectBuffer payload;

    NioFrame(final int headerLength, final int bufferCapacity) {
        this(headerLength, new ExpandableDirectByteBuffer(bufferCapacity));
    }

    NioFrame(final int headerLength, final MutableDirectBuffer buffer) {
        if (headerLength < 0) {
            throw new IllegalArgumentException("Header length cannot be negative: " + headerLength);
        }
        if (headerLength > buffer.capacity()) {
            throw new IllegalArgumentException("Header length cannot exceed buffer capacity: " + headerLength + " > " +
                    buffer.capacity());
        }
        this.headerLength = headerLength;
        this.buffer = requireNonNull(buffer);
        this.header = new UnsafeBuffer(buffer, 0, headerLength);
        this.payload = new UnsafeBuffer(buffer, headerLength, buffer.capacity() - headerLength);
    }

    int headerLength() {
        return headerLength;
    }

    MutableDirectBuffer frame() {
        return buffer;
    }

    MutableDirectBuffer header() {
        return header;
    }

    MutableDirectBuffer payload() {
        return payload;
    }
}
