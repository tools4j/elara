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

import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.tcp.ClientMessageReceiver;
import org.tools4j.elara.stream.tcp.TcpConnection.ClientConnection;
import org.tools4j.elara.stream.tcp.TcpEndpoints;

import java.net.SocketAddress;

import static java.util.Objects.requireNonNull;

public class TcpClientConnection implements ClientConnection {

    private final ConnectingMessageReceiver receiver;
    private final TcpReconnectingSender sender = new TcpReconnectingSender();

    public TcpClientConnection(final SocketAddress connectAddress) {
        this.receiver = new ConnectingMessageReceiver(connectAddress);
    }

    @Override
    public MessageSender sender() {
        return sender;
    }

    @Override
    public ClientMessageReceiver receiver() {
        return receiver;
    }

    @Override
    public boolean isConnected() {
        return sender.isConnected();
    }

    @Override
    public boolean isClosed() {
        return receiver.isClosed();
    }

    @Override
    public void close() {
        receiver.close();
    }

    private class ConnectingMessageReceiver implements ClientMessageReceiver {
        final SocketAddress connecctAddress;
        ClientPoller poller;
        ConnectHandler connectHandler;
        final ConnectHandler senderInitilisingHandler = this::onConnect;
        ConnectingMessageReceiver(final SocketAddress connecctAddress) {
            this.connecctAddress = requireNonNull(connecctAddress);
            poller = new ClientPoller(connecctAddress);
        }

        void reconnect() {
            if (poller != null) {
                try {
                    poller.close();
                } catch (final Exception e) {
                    //ignore
                }
                poller = new ClientPoller(connecctAddress);
                sender.init();
            }
        }

        @Override
        public int poll(final ConnectHandler connectHandler, final Handler messageHandler) {
            this.connectHandler = connectHandler;
            try {
                return poll(messageHandler);
            } finally {
                this.connectHandler = null;
            }
        }

        @Override
        public int poll(final Handler handler) {
            if (poller != null) {
                if (sender.isFailed()) {
                    reconnect();
                }
                return poller.poll(senderInitilisingHandler, handler);
            }
            return 0;
        }

        @Override
        public boolean isClosed() {
            return poller == null;
        }

        @Override
        public void close() {
            if (poller != null) {
                poller = null;
                sender.close();
            }
        }

        private void onConnect(final TcpEndpoints endpoints) {
            if (!isClosed()) {
                sender.connect((TcpSender)endpoints.sender());
                if (connectHandler != null) {
                    connectHandler.onConnect(endpoints);
                }
            }
        }
    }
}
