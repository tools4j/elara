/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.stream.MessageReceiver.Handler;
import org.tools4j.elara.stream.nio.ChannelBuffers;
import org.tools4j.elara.stream.nio.NioEndpoint;
import org.tools4j.elara.stream.nio.NioFrame;
import org.tools4j.elara.stream.nio.NioHeader;
import org.tools4j.elara.stream.nio.NioPoller;
import org.tools4j.elara.stream.nio.ReadHandler;
import org.tools4j.elara.stream.nio.SelectionHandler;
import org.tools4j.elara.stream.nio.WriteHandler;
import org.tools4j.elara.stream.tcp.AcceptListener;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class TcpServerEndpoint implements NioEndpoint {

    private static final int CHANNELS = 8;

    private final TcpServer server;
    private final ServerSocketChannel serverSocketChannel;
    private final AcceptListener acceptListener;
    private final ReadHandler readSelectionHandler;
    private final WriteHandler writeSelectionHandler;
    private final SelectionHandler receiverAcceptHandler = key -> onSelectionKey(key, SelectionKey.OP_READ);
    private final SelectionHandler senderAcceptHandler = key -> onSelectionKey(key, SelectionKey.OP_WRITE);
    private final NioPoller receiverPoller = new NioPoller();
    private final NioPoller senderPoller = new NioPoller();
    private final List<SocketChannel> accepted = new ArrayList<>();

    TcpServerEndpoint(final TcpServer server,
                      final SocketAddress bindAddress,
                      final AcceptListener acceptListener,
                      final Supplier<? extends ByteBuffer> bufferFactory) {
        try {
            this.server = requireNonNull(server);
            this.serverSocketChannel = ServerSocketChannel.open()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true);
            this.acceptListener = requireNonNull(acceptListener);
            this.readSelectionHandler = new ReadHandler(new ChannelBuffers(CHANNELS, bufferFactory), receiverAcceptHandler);
            this.writeSelectionHandler = new WriteHandler(new ChannelBuffers(CHANNELS, bufferFactory), senderAcceptHandler);
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(senderPoller.selector(), SelectionKey.OP_ACCEPT);
            this.serverSocketChannel.register(receiverPoller.selector(), SelectionKey.OP_ACCEPT);
            this.serverSocketChannel.bind(bindAddress);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    List<SocketChannel> acceptedClientChannels() {
        return accepted;
    }

    private void onAccept(final ServerSocketChannel serverChannel,
                          final SocketChannel clientChannel,
                          final SelectionKey key) {
        if (!accepted.contains(clientChannel)) {
            accepted.add(clientChannel);
        }
        acceptListener.onAccept(server, serverChannel, clientChannel, key);
    }

    @Override
    public void close() {
        try {
            CloseHelper.closeAll(accepted);
            receiverPoller.close();
            senderPoller.close();
            serverSocketChannel.close();
            accepted.clear();
        } catch (final Exception ex) {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    int poll() {
        try {
            return receiverPoller.selectNow(receiverAcceptHandler) + senderPoller.selectNow(senderAcceptHandler);
        } catch (final IOException e) {
            //FIXME handle more gracefully
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public int receive(final NioHeader header, final Handler messageHandler) throws IOException {
        return receiverPoller.selectNow(readSelectionHandler.initForSelection(header, messageHandler));
    }

    @Override
    public int send(final NioFrame frame) throws IOException {
        return senderPoller.selectNow(writeSelectionHandler.initForSelection(frame));
    }

    private int onSelectionKey(final SelectionKey key, final int registerOps) throws IOException {
        if (key.isAcceptable()) {
            final SocketChannel socketChannel = serverSocketChannel.accept()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                    .setOption(StandardSocketOptions.SO_KEEPALIVE, true);
//                    .setOption(StandardSocketOptions.TCP_NODELAY, true);
            socketChannel.configureBlocking(false);
            final SelectionKey channelKey = socketChannel.register(key.selector(), registerOps);
            onAccept(serverSocketChannel, socketChannel, channelKey);
        }
        return SelectionHandler.OK;
    }

    @Override
    public boolean isConnected() {
        boolean connected = false;
        for (int i = accepted.size() - 1; i >= 0; i--) {
            final SocketChannel channel = accepted.get(i);
            if (channel.isConnected()) {
                connected = true;
            } else {
                CloseHelper.quietClose(accepted.remove(i));
            }
        }
        return connected;
    }
    @Override
    public boolean isClosed() {
        return !serverSocketChannel.isOpen();
    }

    @Override
    public String toString() {
        return "TcpServer{" + serverSocketChannel.socket().getLocalSocketAddress() + '}';
    }
}