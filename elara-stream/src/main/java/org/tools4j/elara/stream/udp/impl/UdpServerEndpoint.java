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
package org.tools4j.elara.stream.udp.impl;

import org.agrona.DirectBuffer;
import org.agrona.LangUtil;
import org.tools4j.elara.stream.MessageReceiver.Handler;
import org.tools4j.elara.stream.nio.NioEndpoint;
import org.tools4j.elara.stream.nio.ReceiverPoller;
import org.tools4j.elara.stream.nio.RingBuffer;
import org.tools4j.elara.stream.nio.SelectionHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class UdpServerEndpoint implements NioEndpoint {

    private final DatagramChannel datagramChannel;
    private final ReceiverPoller receiverPoller;

    UdpServerEndpoint(final SocketAddress bindAddress, final ReceiverPoller receiverPoller) {
        try {
            this.datagramChannel = DatagramChannel.open()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true);
            this.datagramChannel.configureBlocking(false);
            this.datagramChannel.register(receiverPoller.selector(), SelectionKey.OP_READ);
            this.datagramChannel.bind(bindAddress);
            this.receiverPoller = requireNonNull(receiverPoller);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public void close() {
        try {
            receiverPoller.close();
            datagramChannel.close();
        } catch (final Exception ex) {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    @Override
    public int receive(final Handler messageHandler) throws IOException {
        return receiverPoller.selectNow(SelectionHandler.NO_OP, messageHandler);
    }

    @Override
    public int send(final DirectBuffer buffer, final int offset, final int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        return datagramChannel.isConnected();
    }

    @Override
    public boolean isClosed() {
        return !datagramChannel.isOpen();
    }

    @Override
    public String toString() {
        return "TcpServer{" + datagramChannel.socket().getLocalSocketAddress() + '}';
    }
}