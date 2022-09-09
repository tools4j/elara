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
import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.nio.TransportPoller;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

abstract class TcpPoller extends TransportPoller {

    private final MutableDirectBuffer directBuffer = new UnsafeBuffer(0, 0);

    private ByteBuffer byteBuffer = BufferUtil.allocateDirectAligned(4096, 64);//FIXME make capacity configurable

    protected final int selectNow(final MessageReceiver.Handler messageHandler) {
        return selectNow(messageHandler, null, 0, 0);
    }

    protected final int selectNow(final DirectBuffer buffer, final int offset, final int length) {
        return selectNow(null, buffer, offset, length);
    }

    private int selectNow(final MessageReceiver.Handler messageHandler,
                          final DirectBuffer buffer, final int offset, final int length) {
        try {
            selector.selectNow();
            final int size = selectedKeySet.size();
            if (size == 0) {
                return 0;
            }
            int result = 0;
            ByteBuffer byteBuffer = null;
            IOException exception = null;
            for (int i = 0; i < size; i++) {
                final SelectionKey key = selectedKeySet.keys()[i];
                result |= key.readyOps();
                try {
                    byteBuffer = onSelectionKey(key, messageHandler, buffer, offset, length, byteBuffer);
                } catch (final IOException e) {
                    exception = exceptionOrSuppress(exception, e);
                }
            }
            selectedKeySet.clear();
            if (exception != null) {
                throw exception;
            }
            return result;
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
        }
        return 0;
    }

    private ByteBuffer onSelectionKey(final SelectionKey key,
                                      final Handler handler,
                                      final DirectBuffer buffer, final int offset, final int length,
                                      final ByteBuffer byteBufferOrNull) throws IOException {
        onSelectionKey(key);
        if (key.isReadable() && handler != null) {
            read(key, handler);
        }
        if (key.isWritable() && buffer != null) {
            final ByteBuffer byteBuffer = byteBufferOrNull != null ? byteBufferOrNull :
                    toByteBuffer(buffer, offset, length);
            write(key, byteBuffer);
            return byteBuffer;
        }
        return byteBufferOrNull;
    }

    abstract protected void onSelectionKey(SelectionKey key) throws IOException;

    private void read(final SelectionKey key, final MessageReceiver.Handler handler) throws IOException{
        final SocketChannel remote = (SocketChannel) key.channel();
        byteBuffer.clear();
        final int bytes = remote.read(byteBuffer);
        if (bytes > 0) {
            try {
                directBuffer.wrap(byteBuffer, 0, bytes);
                handler.onMessage(directBuffer);
            } finally {
                directBuffer.wrap(0, 0);
                byteBuffer.clear();
            }
        }
    }

    private void write(final SelectionKey key, final ByteBuffer buffer) throws IOException {
        final SocketChannel remote = (SocketChannel) key.channel();
        if (buffer.remaining() > 0) {
            final int limit = buffer.limit();
            final int pos = buffer.position();
            try {
                remote.write(buffer);
            } finally {
                buffer.limit(limit);
                buffer.position(pos);
            }
        }
    }

    private ByteBuffer toByteBuffer(final DirectBuffer buffer, final int offset, final int length) {
        ByteBuffer buf = buffer.byteBuffer();
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
            buf.put(arr, adj + offset, length);
            buf.limit(length);
            buf.position(0);
        }
        return buf;
    }

    public boolean isClosed() {
        return !selector.isOpen();
    }

    private static <T extends Throwable> T exceptionOrSuppress(final T throwable, final T next) {
        if (throwable == null) {
            return next;
        }
        throwable.addSuppressed(next);
        return throwable;
    }
}