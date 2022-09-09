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

import org.agrona.CloseHelper;
import org.agrona.LangUtil;
import org.tools4j.elara.stream.tcp.ConnectListener;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static java.util.Objects.requireNonNull;

final class ClientPoller extends TcpPoller {

    private final SocketChannel socketChannel;
    private final ConnectListener connectListener;

    ClientPoller(final SocketAddress connectAddress, final ConnectListener connectListener) {
        try {
            this.socketChannel = SocketChannel.open();
            this.connectListener = requireNonNull(connectListener);
            this.socketChannel.configureBlocking(false);
            this.socketChannel.register(selector, SelectionKey.OP_CONNECT);
            this.socketChannel.connect(connectAddress);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public void close() {
        super.close();
        CloseHelper.quietClose(socketChannel);
    }

    @Override
    protected void onSelectionKey(final SelectionKey key) throws IOException {
        if (key.isConnectable() && socketChannel.finishConnect()) {
            socketChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            connectListener.onConnect(socketChannel);
        }
    }

    public boolean isConnected() {
        return socketChannel.isConnected();
    }

    @Override
    public String toString() {
        return "ClientPoller{" + socketChannel.socket().getLocalAddress() + '}';
    }
}