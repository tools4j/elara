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
package org.tools4j.elara.websocket;

import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.IntArrayQueue;
import org.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.LockSupport;

import static java.lang.Math.max;

final class ByteFragmentStore {
    private static final int PART_FLAG = 1 << 31;
    private static final int NULL = PART_FLAG | Integer.MAX_VALUE;
    private final MutableDirectBuffer data;
    private final IntArrayQueue lengths;
    private int readOffset;
    private int writeOffset;

    ByteFragmentStore(final int dataCapacity, final int initialMessageCount) {
        this.data = new UnsafeBuffer(BufferUtil.allocateDirectAligned(dataCapacity, 64));
        this.lengths = new IntArrayQueue(initialMessageCount, NULL);
    }

    synchronized void add(final ByteBuffer partialMessage, final boolean last) {
        final int length = partialMessage.remaining();
        if (length > 0) {
            if (length > data.capacity() || length == Integer.MAX_VALUE) {
                //drop message, too large
                throw new IllegalArgumentException("message too large: " + length + " exceeds buffer capacity "
                        + data.capacity());
            }
            if (writeOffset >= readOffset && writeOffset + length > data.capacity()) {
                writeOffset = 0;
            }
            while (writeOffset + length > Math.min(readOffset, data.capacity())) {
                if (Thread.interrupted()) {
                    Thread.currentThread().interrupt();
                    return;
                }
                LockSupport.parkNanos(10_000_000);//make configurable
            }
            data.putBytes(writeOffset, partialMessage, partialMessage.position(), length);
            writeOffset += length;
        }
        lengths.addInt(last ? length : length | PART_FLAG);
    }

    synchronized DirectBuffer readNext(final DirectBuffer view) {
        final int length = lengths.pollInt();
        if (length == NULL) {
            return null;
        }
        if (length >= 0) {
            if (readOffset + length > data.capacity()) {
                readOffset = 0;
            }
            view.wrap(data, readOffset, length);
            return view;
        }
        //fragmented parts
        int length1 = length ^ PART_FLAG;
        int length2 = -1;
        if (readOffset + length1 > data.capacity()) {
            readOffset = 0;
        }
        int fragmentLen = lengths.pollInt();
        while (fragmentLen < 0) {
            if (fragmentLen == NULL) {
                //fragments are incomplete, add 1 or 2 combined fragments back to queue
                lengths.addInt(length1 | PART_FLAG);
                if (length2 >= 0) {
                    lengths.addInt(length2 | PART_FLAG);
                }
                return null;
            }
            final int len = (fragmentLen ^ PART_FLAG);
            if (length2 < 0 && readOffset + length1 + len <= data.capacity()) {
                length1 += len;
            } else {
                length2 = max(length2, 0) + len;
            }
            fragmentLen = lengths.pollInt();
        }
        if (length2 < 0) {
            view.wrap(data, readOffset, length1);
            return view;
        }
        //fragmented message is in 2 parts at end and at start of data buffer
        //we need to re-assemble
        final MutableDirectBuffer tempBuffer = new ExpandableArrayBuffer(length1 + length2);
        tempBuffer.putBytes(0, data, readOffset, length1);
        tempBuffer.putBytes(0, data, 0, length2);
        view.wrap(tempBuffer);
        readOffset = length2;
        return view;
    }

    synchronized void consume(final DirectBuffer view) {
        if (view.byteBuffer() == data.byteBuffer() || view.byteArray() == data.byteArray()) {
            readOffset += view.capacity();
        }
        view.wrap(0, 0);
    }
}
