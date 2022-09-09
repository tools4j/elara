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
import org.tools4j.elara.stream.tcp.ConnectListener;
import org.tools4j.elara.stream.tcp.TcpConnection;

import java.net.SocketAddress;
import java.nio.channels.SelectionKey;

import static java.util.Objects.requireNonNull;

public class TcpClientConnection implements TcpConnection {

    private final SocketAddress connectAddress;
    private final ConnectListener connectListener;
    private final TcpClientReceiver receiver;
    private final TcpClientSender sender;
    private ClientPoller poller;

    public TcpClientConnection(final SocketAddress connectAddress, final ConnectListener connectListener) {
        this.connectAddress = requireNonNull(connectAddress);
        this.connectListener = requireNonNull(connectListener);
        this.receiver = new TcpClientReceiver();
        this.sender = new TcpClientSender();
        this.poller  = new ClientPoller(connectAddress, connectListener);
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
        return poller != null && poller.isConnected();
    }

    @Override
    public boolean isClosed() {
        return poller == null;
    }

    @Override
    public void close() {
        if (poller != null) {
            CloseHelper.quietClose(poller);
            poller = null;
        }
    }

    private void reconnect() {
        if (poller != null) {
            CloseHelper.quietClose(poller);
            poller = new ClientPoller(connectAddress, connectListener);
        }
    }

    private final class TcpClientReceiver implements MessageReceiver {
        @Override
        public int poll(final Handler handler) {
            if (poller != null) {
                try {
                    return 0 != (poller.selectNow(handler) & SelectionKey.OP_READ) ? 1 : 0;
                } catch (final Exception e) {
                    reconnect();
                    //FIXME log
                    throw e;
                }
            }
            return 0;
        }

        @Override
        public boolean isClosed() {
            return TcpClientConnection.this.isClosed();
        }

        @Override
        public void close() {
            TcpClientConnection.this.close();
        }

        @Override
        public String toString() {
            return "TcpClientReceiver{" + connectAddress + '}';
        }
    }

    @Override
    public String toString() {
        return "TcpClientConnection{" + connectAddress + '}';
    }

    private final class TcpClientSender extends MessageSender.Buffered {
        @Override
        public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
            if (isClosed()) {
                return SendingResult.CLOSED;
            }
            try {
                final int readyOps = poller.selectNow(buffer, offset, length);
                if (0 != (readyOps & SelectionKey.OP_WRITE)) {
                    return SendingResult.SENT;
                }
                return isConnected() ? SendingResult.BACK_PRESSURED : SendingResult.DISCONNECTED;
            } catch (final Exception e) {
                reconnect();
                //FIXME log
                return SendingResult.FAILED;
            }
        }

        @Override
        public boolean isClosed() {
            return TcpClientConnection.this.isClosed();
        }

        @Override
        public void close() {
            TcpClientConnection.this.close();
        }

        @Override
        public String toString() {
            return "TcpClientSender{" + connectAddress + '}';
        }
    }
}
