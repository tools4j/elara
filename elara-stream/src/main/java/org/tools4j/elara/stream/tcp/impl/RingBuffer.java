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
package org.tools4j.elara.stream.tcp.impl;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

interface RingBuffer {

    int capacity();

    int readOffset();
    int writeOffset();
    int readLength();
    int writeLength();

    void readOffset(int offset);
    void writeOffset(int offset);
    void reset();
    void reset(int readOffset, int writeOffset);

    int readUnsignedByte(boolean commit);
    long readUnsignedInt(boolean commit);
    int read(MutableDirectBuffer target, int targetOffset, int maxLength);
    int read(ByteBuffer target, int targetOffset, int maxLength);
    boolean readWrap(DirectBuffer wrap);
    boolean readWrap(DirectBuffer wrap, int maxLength);
    ByteBuffer readOrNull();
    void readCommit(int bytes);
    boolean readSkip(int bytes);
    boolean readSkipEndGap(int minBytes);

    boolean writeUnsignedByte(byte value);
    boolean writeUnsignedInt(int value);
    boolean write(DirectBuffer source, int offset, int length);
    boolean write(ByteBuffer source, int offset, int length);
    boolean writeWrap(MutableDirectBuffer wrap);
    boolean writeWrap(MutableDirectBuffer wrap, int maxLength);
    ByteBuffer writeOrNull();
    void writeCommit(int bytes);
    boolean writeSkip(int bytes);
    boolean writeSkipEndGap(int minBytes);

    static Supplier<RingBuffer> factory(final int bufferSize) {
        RingBufferImpl.validateBufferSize(bufferSize);
        return () -> new RingBufferImpl(bufferSize);
    }
}
