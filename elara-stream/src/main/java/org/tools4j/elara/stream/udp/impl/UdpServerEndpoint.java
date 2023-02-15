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

import org.agrona.LangUtil;
import org.tools4j.elara.stream.MessageReceiver.Handler;
import org.tools4j.elara.stream.nio.ChannelBuffers;
import org.tools4j.elara.stream.nio.NioEndpoint;
import org.tools4j.elara.stream.nio.NioFrame;
import org.tools4j.elara.stream.nio.NioHeader;
import org.tools4j.elara.stream.nio.NioPoller;
import org.tools4j.elara.stream.nio.ReadHandler;
import org.tools4j.elara.stream.nio.WriteHandler;
import org.tools4j.elara.stream.udp.RemoteAddressListener;
import org.tools4j.elara.stream.udp.UdpSendingStrategy;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.lang.Math.max;
import static java.util.Objects.requireNonNull;

final class UdpServerEndpoint implements NioEndpoint {

    private final int CHANNEL_CAPACITY = 8;
    private final UdpServer server;
    private final DatagramChannel datagramChannel;
    private final RemoteAddressListener remoteAddressListener;
    private final UdpSendingStrategy sendingStrategy;
    private final ReadHandler readSelectionHandler;
    private final WriteHandler writeSelectionHandler;
    private final NioPoller receiverPoller = new NioPoller();
    private final NioPoller senderPoller = new NioPoller();
    private final List<SocketAddress> remoteAddresses = new ArrayList<>();

    UdpServerEndpoint(final UdpServer server,
                      final SocketAddress bindAddress,
                      final RemoteAddressListener remoteAddressListener,
                      final UdpSendingStrategy sendingStrategy,
                      final int mtuLength,
                      final Supplier<? extends ByteBuffer> bufferFactory) {
        try {
            this.server = requireNonNull(server);
            this.datagramChannel = DatagramChannel.open()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true);
            this.remoteAddressListener = requireNonNull(remoteAddressListener);
            this.sendingStrategy = requireNonNull(sendingStrategy);
            this.readSelectionHandler = new UdpReadHandler(bufferFactory);
            this.writeSelectionHandler = new UdpWriteHandler(bufferFactory, mtuLength);
            this.datagramChannel.configureBlocking(false);
            this.datagramChannel.register(receiverPoller.selector(), SelectionKey.OP_READ);
            this.datagramChannel.register(senderPoller.selector(), SelectionKey.OP_WRITE);
            this.datagramChannel.bind(bindAddress);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    List<SocketAddress> remoteAddresses() {
        return remoteAddresses;
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
        return receiverPoller.selectNow(readSelectionHandler.initForSelection(header, messageHandler));
    }

    @Override
    public int send(final NioFrame frame) throws IOException {
        return senderPoller.selectNow(writeSelectionHandler.initForSelection(frame));
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

    private class UdpReadHandler extends ReadHandler {
        public UdpReadHandler(final Supplier<? extends ByteBuffer> bufferFactory) {
            super(new ChannelBuffers(CHANNEL_CAPACITY, bufferFactory));
        }

        @Override
        protected int readFromChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
            final DatagramChannel datagramChannel = (DatagramChannel) channel;
            final int pos = buffer.position();
            final SocketAddress sourceAddress = datagramChannel.receive(buffer);
            if (sourceAddress != null) {
                if (!remoteAddresses.contains(sourceAddress)) {
                    remoteAddresses.add(sourceAddress);
                    remoteAddressListener.onRemoteAddressAdded(server, datagramChannel, sourceAddress);
                }
                return buffer.position() - pos;
            }
            return 0;
        }
    }

    private class UdpWriteHandler extends UdpWriteSelectionHandler {
        public UdpWriteHandler(final Supplier<? extends ByteBuffer> bufferFactory, final int mtuLength) {
            super(new ChannelBuffers(CHANNEL_CAPACITY, bufferFactory), mtuLength);
        }

        @Override
        protected int writeToChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
            final DatagramChannel datagramChannel = (DatagramChannel) channel;
            final int remotes = remoteAddresses.size();
            int sent = 0;
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < remotes; i++) {
                final SocketAddress remoteAddress = sendingStrategy.nextRecipient(server, i);
                if (remoteAddress == null) {
                    break;
                }
                sent = max(sent, sendFragments(datagramChannel, remoteAddress, buffer));
            }
            return sent;
        }

    }
}