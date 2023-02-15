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
package org.tools4j.elara.stream.tcp.impl;

import org.agrona.LangUtil;
import org.tools4j.elara.stream.MessageReceiver.Handler;
import org.tools4j.elara.stream.nio.ChannelBuffers;
import org.tools4j.elara.stream.nio.NioEndpoint;
import org.tools4j.elara.stream.nio.NioFrame;
import org.tools4j.elara.stream.nio.NioHeader;
import org.tools4j.elara.stream.nio.NioPoller;
import org.tools4j.elara.stream.nio.ReadHandler;
import org.tools4j.elara.stream.nio.SelectionHandler;
import org.tools4j.elara.stream.nio.WriteHandler;
import org.tools4j.elara.stream.tcp.ConnectListener;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class TcpClientEndpoint implements NioEndpoint {

    private final TcpClient client;
    private final SocketChannel socketChannel;
    private final ConnectListener connectListener;
    private final ReadHandler readHandler;
    private final WriteHandler writeHandler;
    private final SelectionHandler connectHandler = this::onSelectionKey;
    private final NioPoller receiverPoller = new NioPoller();
    private final NioPoller senderPoller = new NioPoller();

    TcpClientEndpoint(final TcpClient client,
                      final SocketAddress connectAddress,
                      final ConnectListener connectListener,
                      final Supplier<? extends ByteBuffer> bufferFactory) {
        try {
            this.client = requireNonNull(client);
            this.socketChannel = SocketChannel.open()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    .setOption(StandardSocketOptions.SO_KEEPALIVE, true)
                    .setOption(StandardSocketOptions.TCP_NODELAY, true);
            this.connectListener = requireNonNull(connectListener);
            this.readHandler = new ReadHandler(new ChannelBuffers(1, bufferFactory), connectHandler);
            this.writeHandler = new WriteHandler(new ChannelBuffers(1, bufferFactory), connectHandler);
            this.socketChannel.configureBlocking(false);
            this.socketChannel.register(receiverPoller.selector(), SelectionKey.OP_CONNECT + SelectionKey.OP_READ);
            this.socketChannel.register(senderPoller.selector(), SelectionKey.OP_CONNECT + SelectionKey.OP_WRITE);
            this.socketChannel.connect(connectAddress);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public void close() {
        try {
            receiverPoller.close();
            senderPoller.close();
            socketChannel.close();
        } catch (final Exception ex) {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    int poll() {
        try {
            return receiverPoller.selectNow(connectHandler) + senderPoller.selectNow(connectHandler);
        } catch (final IOException e) {
            //FIXME handle more gracefully
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public int receive(final NioHeader header, final Handler messageHandler) throws IOException {
        return receiverPoller.selectNow(readHandler.initForSelection(header, messageHandler));
    }

    @Override
    public int send(final NioFrame frame) throws IOException {
        return senderPoller.selectNow(writeHandler.initForSelection(frame));
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        if (key.isConnectable() && socketChannel.finishConnect()) {
            connectListener.onConnect(client, socketChannel, key);
        }
        return SelectionHandler.OK;
    }

    public boolean isConnected() {
        return socketChannel.isConnected();
    }

    @Override
    public boolean isClosed() {
        return !socketChannel.isOpen();
    }

    @Override
    public String toString() {
        return "TcpClient{" + socketChannel.socket().getLocalAddress() + '}';
    }
}