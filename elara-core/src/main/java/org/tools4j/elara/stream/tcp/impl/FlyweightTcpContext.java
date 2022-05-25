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
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.tcp.TcpEndpoints;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

import static java.util.Objects.requireNonNull;

final class FlyweightTcpContext implements TcpEndpoints {

    private SocketChannel socketChannel;
    private TcpSender sender;

    FlyweightTcpContext init(final SocketChannel socketChannel) {
        this.socketChannel = requireNonNull(socketChannel);
        return this;
    }

    void reset() {
        socketChannel = null;
        sender = null;
    }

    @Override
    public MessageSender sender() {
        if (sender == null) {
            sender = new TcpSender(socketChannel);
        }
        return sender;
    }

    @Override
    public void close() {
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (final IOException e) {
                LangUtil.rethrowUnchecked(e);
            } finally {
                reset();
            }
        }
    }

    @Override
    public SocketAddress localAddress() {
        try {
            return socketChannel.getLocalAddress();
        } catch (final IOException e) {
            throw new RuntimeException("local address not available, e=" + e, e);
        }
    }

    @Override
    public SocketAddress remoteAddress() {
        try {
            return socketChannel.getRemoteAddress();
        } catch (final IOException e) {
            throw new RuntimeException("remote address not available, e=" + e, e);
        }
    }
}
