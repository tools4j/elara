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
import org.agrona.DirectBuffer;
import org.tools4j.elara.send.SendingResult;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.tcp.AcceptListener;
import org.tools4j.elara.stream.tcp.TcpConnection;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class TcpServerConnection implements TcpConnection {

    private final SocketAddress bindAddress;
    private final AcceptListener acceptListener;
    private final TcpMulticastSender sender;
    private final TcpServerReceiver receiver;
    private final AcceptListener acceptHandler = this::onAccept;
    private final List<SocketChannel> accepted = new ArrayList<>();
    private ServerPoller poller;

    public TcpServerConnection(final SocketAddress bindAddress, final AcceptListener acceptListener) {
        this.bindAddress = requireNonNull(bindAddress);
        this.acceptListener = requireNonNull(acceptListener);
        this.sender = new TcpMulticastSender();
        this.receiver = new TcpServerReceiver();
        this.poller = new ServerPoller(bindAddress, acceptHandler);
    }

    @Override
    public MessageSender sender() {
        return sender;
    }

    @Override
    public MessageReceiver receiver() {
        return receiver;
    }

    @Override
    public boolean isConnected() {
        if (poller == null || accepted.isEmpty()) {
            return false;
        }
        boolean connected = false;
        for (int i = 0; i < accepted.size(); ) {
            final SocketChannel channel = accepted.get(i);
            if (channel.isConnected()) {
                connected = true;
                i++;
            } else {
                accepted.remove(i);
                CloseHelper.quietClose(channel);
            }
        }
        return connected;
    }

    @Override
    public boolean isClosed() {
        return poller == null;
    }

    @Override
    public void close() {
        if (poller != null) {
            CloseHelper.quietCloseAll(accepted);
            accepted.clear();
            CloseHelper.quietClose(poller);
            poller = null;
        }
    }

    private void onAccept(final ServerSocketChannel serverChannel, final SocketChannel clientChannel) {
        accepted.add(clientChannel);
        acceptListener.onAccept(serverChannel, clientChannel);
    }

    @Override
    public String toString() {
        return "TcpServerConnection{" + bindAddress + '}';
    }

    private final class TcpServerReceiver implements MessageReceiver {
        @Override
        public int poll(final Handler handler) {
            if (poller != null) {
                return 0 != (poller.selectNow(handler) & SelectionKey.OP_READ) ? 1 : 0;
            }
            return 0;
        }

        @Override
        public boolean isClosed() {
            return TcpServerConnection.this.isClosed();
        }

        @Override
        public void close() {
            TcpServerConnection.this.close();
        }

        @Override
        public String toString() {
            return "TcpServerReceiver{" + bindAddress + '}';
        }
    }

    private final class TcpMulticastSender extends MessageSender.Buffered {
        @Override
        public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
            final int readyOps = poller.selectNow(buffer, offset, length);
            if (0 != (readyOps & SelectionKey.OP_WRITE)) {
                return SendingResult.SENT;
            }
            if (isConnected()) {
                return SendingResult.BACK_PRESSURED;
            }
            return isClosed() ? SendingResult.CLOSED : SendingResult.DISCONNECTED;
        }


        @Override
        public boolean isClosed() {
            return TcpServerConnection.this.isClosed();
        }

        @Override
        public void close() {
            TcpServerConnection.this.close();
        }

        @Override
        public String toString() {
            return "TcpServerSender{" + bindAddress + '}';
        }
    }
}
