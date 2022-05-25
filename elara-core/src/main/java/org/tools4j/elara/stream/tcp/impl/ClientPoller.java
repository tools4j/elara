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

import org.agrona.LangUtil;
import org.tools4j.elara.stream.tcp.ClientMessageStream;
import org.tools4j.elara.stream.tcp.ServerMessageStream.AcceptHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

final class ClientPoller extends TcpPoller implements ClientMessageStream {

    private final SocketChannel socketChannel;
    private final FlyweightTcpContext context = new FlyweightTcpContext();

    ClientPoller(final SocketAddress connectAddress) {
        try {
            this.socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(connectAddress);
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public int poll(final Handler handler) {
        return poll(endpoints -> {}, handler);
    }

    @Override
    public int poll(final ConnectHandler connectHandler, final Handler messageHandler) {
        return selectNow(endpoints -> {}, connectHandler, messageHandler);
    }

    @Override
    protected void onSelectionKey(final SelectionKey key, final AcceptHandler acceptHandler, final ConnectHandler connectHandler, final Handler messageHandler) throws IOException {
        if (key.isConnectable()) {
            finishConnect(connectHandler);
        }
    }

    private void finishConnect(final ConnectHandler connectHandler) throws IOException {
        if (socketChannel.finishConnect()) {
            connectHandler.onConnect(context.init(socketChannel));
        }
    }

}