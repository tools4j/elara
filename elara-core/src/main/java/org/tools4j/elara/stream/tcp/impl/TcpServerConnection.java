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
import org.tools4j.elara.stream.tcp.ServerMessageStream;
import org.tools4j.elara.stream.tcp.TcpConnection.ServerConnection;
import org.tools4j.elara.stream.tcp.TcpEndpoints;

import java.net.SocketAddress;

public class TcpServerConnection implements ServerConnection {

    private final TcpMulticastSender sender;
    private final AcceptingMessageStream stream;

    public TcpServerConnection(final SocketAddress bindAddress) {
        this.sender = new TcpMulticastSender();
        this.stream = new AcceptingMessageStream(bindAddress);
    }

    @Override
    public MessageSender sender() {
        return sender;
    }

    @Override
    public ServerMessageStream stream() {
        return stream;
    }

    @Override
    public boolean isConnected() {
        return sender.isConnected();
    }

    @Override
    public boolean isClosed() {
        return stream.isClosed();
    }

    @Override
    public void close() {
        if (stream.close()) {
            sender.removeAll();
        }
    }

    private class AcceptingMessageStream implements ServerMessageStream {
        ServerPoller poller;
        AcceptHandler acceptHandler;
        final AcceptHandler senderInitilisingHandler = this::onAccept;
        AcceptingMessageStream(final SocketAddress bindAddress) {
            poller = new ServerPoller(bindAddress);
        }

        @Override
        public int poll(final AcceptHandler acceptHandler, final Handler messageHandler) {
            this.acceptHandler = acceptHandler;
            try {
                return poll(messageHandler);
            } finally {
                this.acceptHandler = null;
            }
        }

        @Override
        public int poll(final Handler handler) {
            if (poller != null) {
                return poller.poll(senderInitilisingHandler, handler);
            }
            return 0;
        }

        boolean isClosed() {
            return poller == null;
        }

        boolean close() {
            if (poller == null) {
                return false;
            }
            poller = null;
            return true;
        }

        private void onAccept(final TcpEndpoints endpoints) {
            if (!isClosed()) {
                sender.add((TcpSender)endpoints.sender());
                if (acceptHandler != null) {
                    acceptHandler.onAccept(endpoints);
                }
            }
        }
    }
}
