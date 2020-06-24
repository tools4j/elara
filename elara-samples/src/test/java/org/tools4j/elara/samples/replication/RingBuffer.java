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
package org.tools4j.elara.samples.replication;

import java.util.Arrays;

public class RingBuffer implements Buffer {

    private final long[] values;
    private int writeOffset;
    private int readOffset;
    private int n;

    public RingBuffer(final int capacity) {
        this.values = new long[capacity];
        Arrays.fill(values, NULL_VALUE);
    }

    @Override
    public synchronized boolean offer(final long value) {
        if (n == values.length) {
            return false;
        }
        values[writeOffset] = value;
        n++;
        writeOffset++;
        if (writeOffset == values.length) {
            writeOffset = 0;
        }
        return true;
    }

    @Override
    public synchronized long consume() {
        if (n == 0) {
            return NULL_VALUE;
        }
        final long value = values[readOffset];
        values[readOffset] = NULL_VALUE;
        n--;
        readOffset++;
        if (readOffset == values.length) {
            readOffset = 0;
        }
        return value;
    }

}
