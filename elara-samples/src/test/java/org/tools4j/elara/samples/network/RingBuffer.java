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
package org.tools4j.elara.samples.network;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

public class RingBuffer implements Buffer {

    private final MutableDirectBuffer[] buffers;
    private int writeOffset;
    private int readOffset;
    private int n;

    public RingBuffer(final int ringCapacity, final int initialBufferCapacity) {
        this.buffers = new MutableDirectBuffer[ringCapacity];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new ExpandableArrayBuffer(initialBufferCapacity);
        }
    }

    @Override
    public synchronized boolean offer(final DirectBuffer src, final int offset, final int length) {
        if (n == buffers.length) {
            return false;
        }
        buffers[writeOffset].putInt(0, length);
        buffers[writeOffset].putBytes(Integer.BYTES, src, offset, length);
        n++;
        writeOffset++;
        if (writeOffset == buffers.length) {
            writeOffset = 0;
        }
        return true;
    }

    @Override
    public synchronized boolean offer(final Buffer buffer) {
        if (n == buffers.length) {
            return false;
        }
        final int length = buffer.consume(buffers[writeOffset], Integer.BYTES);
        if (length == CONSUMED_NOTHING) {
            return false;
        }
        buffers[writeOffset].putInt(0, length);
        n++;
        writeOffset++;
        if (writeOffset == buffers.length) {
            writeOffset = 0;
        }
        return true;
    }

    @Override
    public synchronized int consume(final MutableDirectBuffer dst, final int dstOffset) {
        if (n == 0) {
            return CONSUMED_NOTHING;
        }
        final int length = buffers[readOffset].getInt(0);
        if (dst != null) {
            dst.putBytes(dstOffset, buffers[readOffset], Integer.BYTES, length);
        }
        buffers[readOffset].setMemory(0, Integer.BYTES + length, (byte)0);
        n--;
        readOffset++;
        if (readOffset == buffers.length) {
            readOffset = 0;
        }
        return length;
    }

    @Override
    public int consume() {
        return consume(null, 0);
    }
}
