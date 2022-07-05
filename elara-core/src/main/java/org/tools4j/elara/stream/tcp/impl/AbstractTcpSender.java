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

import org.agrona.BufferUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.stream.MessageSender;

import java.io.IOException;
import java.nio.ByteBuffer;

abstract class AbstractTcpSender extends MessageSender.Buffered {

    private ByteBuffer byteBuffer;//lazy init only if needed

    AbstractTcpSender() {
        super();
    }

    AbstractTcpSender(final MutableDirectBuffer buffer) {
        super(buffer);
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        if (!isConnected()) {
            return SendingResult.DISCONNECTED;
        }
        ByteBuffer buf = buffer.byteBuffer();
        try {
            if (buf != null) {
                final int adj = buffer.wrapAdjustment();
                buf.limit(adj + offset + length);
                buf.position(adj + offset);
            } else {
                final byte[] arr = buffer.byteArray();
                if (arr == null) {
                    throw new IllegalArgumentException("Unsupported buffer, only ByteBuffer or byte array backed buffers are supported");
                }
                if (byteBuffer == null || byteBuffer.capacity() < length) {
                    buf = byteBuffer = BufferUtil.allocateDirectAligned(Math.max(1024, length), 64);
                } else {
                    buf = byteBuffer;
                }
                buf.clear();
                final int adj = buffer.wrapAdjustment();
                buf.limit(length);
                buf.position(0);
                buf.put(arr, adj + offset, length);
            }
            write(buf);
            return SendingResult.SENT;
        } catch (final IOException e) {
            //FIXME log + handle exception
            return SendingResult.FAILED;
        }
    }

    abstract public boolean isConnected();
    abstract protected void write(ByteBuffer buffer) throws IOException;
}
