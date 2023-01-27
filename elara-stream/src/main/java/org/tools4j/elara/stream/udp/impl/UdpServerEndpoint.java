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
import org.tools4j.elara.stream.nio.NioHeader;
import org.tools4j.elara.stream.nio.NioPoller;
import org.tools4j.elara.stream.nio.ReadSelectionHandler;
import org.tools4j.elara.stream.nio.RingBuffer;
import org.tools4j.elara.stream.nio.WriteSelectionHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static org.tools4j.elara.stream.nio.SelectionHandler.DISCONNECTED;

final class UdpServerEndpoint implements NioEndpoint {

    private final DatagramChannel datagramChannel;
    private final ReadSelectionHandler readSelectionHandler;
    private final WriteSelectionHandler writeSelectionHandler;
    private final NioPoller receiverPoller = new NioPoller();
    private final NioPoller senderPoller = new NioPoller();
    private SocketAddress lastSourceAddress;


    UdpServerEndpoint(final SocketAddress bindAddress,
                      final Supplier<? extends RingBuffer> ringBufferFactory) {
        try {
            this.datagramChannel = DatagramChannel.open()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true);
            this.readSelectionHandler = new UdpReadHandler(ringBufferFactory);
            this.writeSelectionHandler = new UdpWriteHandler(ringBufferFactory);
            this.datagramChannel.configureBlocking(false);
            this.datagramChannel.register(receiverPoller.selector(), SelectionKey.OP_READ);
            this.datagramChannel.bind(bindAddress);
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
    public int receive(final NioHeader header, final Handler messageHandler) throws IOException {
        return receiverPoller.selectNow(readSelectionHandler.init(header, messageHandler));
    }

    @Override
    public int send(final DirectBuffer buffer, final int offset, final int length) throws IOException {
        return senderPoller.selectNow(writeSelectionHandler.init(buffer, offset, length));
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

    private class UdpReadHandler extends ReadSelectionHandler {
        public UdpReadHandler(final Supplier<? extends RingBuffer> ringBufferFactory) {
            super(ringBufferFactory);
        }

        @Override
        protected int readFromChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
            final DatagramChannel datagramChannel = (DatagramChannel) channel;
            final int pos = buffer.position();
            final SocketAddress sourceAddress = datagramChannel.receive(buffer);
            if (sourceAddress != null) {
                lastSourceAddress = sourceAddress;
                return buffer.position() - pos;
            }
            return 0;
        }
    }

    private class UdpWriteHandler extends WriteSelectionHandler {
        public UdpWriteHandler(final Supplier<? extends RingBuffer> ringBufferFactory) {
            super(ringBufferFactory);
        }

        @Override
        protected int writeToChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
            if (lastSourceAddress != null) {
                return DISCONNECTED;
            }
            final DatagramChannel datagramChannel = (DatagramChannel) channel;
            return datagramChannel.send(buffer, lastSourceAddress);
        }
    }
}