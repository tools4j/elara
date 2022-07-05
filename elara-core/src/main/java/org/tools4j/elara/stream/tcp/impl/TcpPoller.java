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
import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.agrona.nio.TransportPoller;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.tcp.ClientMessageReceiver.ConnectHandler;
import org.tools4j.elara.stream.tcp.ServerMessageReceiver.AcceptHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class TcpPoller extends TransportPoller {

    private final MutableDirectBuffer directBuffer = new UnsafeBuffer(0, 0);
    private final ByteBuffer byteBuffer = BufferUtil.allocateDirectAligned(4096, 64);//FIXME make capacity configurable

    protected final int selectNow(final AcceptHandler acceptHandler,
                                  final ConnectHandler connectHandler,
                                  final MessageReceiver.Handler messageHandler) {
        try {
            selector.selectNow();
            final int size = selectedKeySet.size();
            if (size == 0) {
                return 0;
            }
            IOException exception = null;
            for (int i = 0; i < size; i++) {
                try {
                    onSelectionKey(selectedKeySet.keys()[i], acceptHandler, connectHandler, messageHandler);
                } catch (final IOException e) {
                    if (exception == null) {
                        exception = e;
                    } else {
                        e.addSuppressed(e);
                    }
                }
            }
            selectedKeySet.clear();
            if (exception != null) {
                throw exception;
            }
            return size;
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
        }
        return 0;
    }

    protected void onSelectionKey(final SelectionKey key,
                                  final AcceptHandler acceptHandler,
                                  final ConnectHandler connectHandler,
                                  final MessageReceiver.Handler messageHandler) throws IOException {
        if (key.isReadable()) {
            read(key, messageHandler);
        }
    }

    private void read(final SelectionKey key, final MessageReceiver.Handler handler) throws IOException{
        final SocketChannel remote = (SocketChannel) key.channel();
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

    public boolean isClosed() {
        return !selector.isOpen();
    }
}