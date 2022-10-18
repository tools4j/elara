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
import org.agrona.LangUtil;
import org.tools4j.elara.stream.MessageReceiver.Handler;
import org.tools4j.elara.stream.tcp.AcceptListener;
import org.tools4j.elara.stream.tcp.impl.TcpPoller.SelectionHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class TcpServer implements TcpEndpoint {

    private final ServerSocketChannel serverSocketChannel;
    private final AcceptListener acceptListener;
    private final ReceiverPoller receiverPoller;
    private final SenderPoller senderPoller;
    private final SelectionHandler receiverAcceptHandler = key -> onSelectionKey(key, SelectionKey.OP_READ);
    private final SelectionHandler senderAcceptHandler = key -> onSelectionKey(key, SelectionKey.OP_WRITE);

    TcpServer(final SocketAddress bindAddress,
              final AcceptListener acceptListener,
              final Supplier<? extends RingBuffer> ringBufferFactory) {
        try {
            this.serverSocketChannel = ServerSocketChannel.open();
            this.acceptListener = requireNonNull(acceptListener);
            this.receiverPoller = new ReceiverPoller(ringBufferFactory);
            this.senderPoller = new SenderPoller(ringBufferFactory);
            this.serverSocketChannel.configureBlocking(false);
            this.serverSocketChannel.register(senderPoller.selector(), SelectionKey.OP_ACCEPT);
            this.serverSocketChannel.register(receiverPoller.selector(), SelectionKey.OP_ACCEPT);
            this.serverSocketChannel.bind(bindAddress);
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
            serverSocketChannel.close();
        } catch (final Exception ex) {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    @Override
    public int poll() {
        receiverPoller.selectNow(receiverAcceptHandler);
        senderPoller.selectNow(senderAcceptHandler);
        return SelectionHandler.OK;
    }

    @Override
    public int receive(final Handler messageHandler) {
        return receiverPoller.selectNow(receiverAcceptHandler, messageHandler);
    }

    @Override
    public int send(final DirectBuffer buffer, final int offset, final int length) {
        return senderPoller.selectNow(senderAcceptHandler, buffer, offset, length);
    }

    private int onSelectionKey(final SelectionKey key, final int registerOps) throws IOException {
        if (key.isAcceptable()) {
            final SocketChannel socketChannel = serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            final SelectionKey channelKey = socketChannel.register(key.selector(), registerOps);
            acceptListener.onAccept(serverSocketChannel, socketChannel, channelKey);
        }
        return SelectionHandler.OK;
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