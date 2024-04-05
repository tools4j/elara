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
package org.tools4j.elara.stream.udp.impl;

import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;
import org.tools4j.elara.stream.nio.NioReceiver;
import org.tools4j.elara.stream.nio.NioSender;
import org.tools4j.elara.stream.udp.UdpEndpoint;
import org.tools4j.elara.stream.udp.UdpHeader;
import org.tools4j.elara.stream.udp.UdpReceiver;
import org.tools4j.elara.stream.udp.UdpSender;
import org.tools4j.elara.stream.udp.config.UdpServerConfig;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static java.util.Objects.requireNonNull;

public class UdpServer implements UdpEndpoint {

    private final SocketAddress bindAddress;
    private final UdpServerConfig configuration;
    private final UdpServerReceiver receiver;
    private final UdpServerSender sender;

    private final Set<SocketChannel> accepted = new CopyOnWriteArraySet<>();
    private UdpServerEndpoint server;

    public UdpServer(final SocketAddress bindAddress, final UdpServerConfig configuration) {
        configuration.validate();
        this.bindAddress = requireNonNull(bindAddress);
        this.configuration = requireNonNull(configuration);
        this.receiver = new UdpServerReceiver(new MutableUdpHeader());
        this.sender = new UdpServerSender(new MutableUdpHeader());
        this.server = new UdpServerEndpoint(this, bindAddress,
                configuration.remoteAddressListener(),
                configuration.sendingStrategyFactory().create(),
                configuration.mtuLength(),
                () -> ByteBuffer.allocateDirect(configuration.bufferCapacity()));
    }

    public List<SocketAddress> remoteAddresses() {
        return server == null ? Collections.emptyList() : server.remoteAddresses();
    }

    @Override
    public int poll() {
        return receiver.poll(message -> {});//need to poll to receive hello messages from clients
    }

    @Override
    public UdpSender sender() {
        return sender;
    }

    @Override
    public UdpReceiver receiver() {
        return receiver;
    }

    @Override
    public boolean isClosed() {
        return server == null;
    }

    @Override
    public void close() {
        if (server != null) {
            CloseHelper.quietCloseAll(accepted);
            accepted.clear();
            CloseHelper.quietClose(server);
            server = null;
        }
    }

    @Override
    public String toString() {
        return "UdpServer{" + bindAddress + '}';
    }

    private final class UdpServerReceiver extends NioReceiver implements UdpReceiver {
        final UdpHeader header;
        String name;
        UdpServerReceiver(final UdpHeader header) {
            super(() -> server, header);
            this.header = requireNonNull(header);
        }

        @Override
        public UdpHeader header() {
            return header.valid() ? header : null;
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UdpServerReceiver{" + bindAddress + '}');
        }
    }

    private final class UdpServerSender extends NioSender implements UdpSender {
        final MutableUdpHeader header;
        final Sequence sequence = new Sequence();
        String name;
        UdpServerSender(final MutableUdpHeader header) {
            super(() -> server, configuration.bufferCapacity(), header);
            this.header = requireNonNull(header);
        }

        @Override
        public long sequence() {
            return sequence.get();
        }

        @Override
        protected void writeHeader(final MutableNioHeader header, final int payloadLength) {
            assert this.header == header;
            super.writeHeader(header, payloadLength);
            this.header.sequence(sequence.get());
        }

        @Override
        public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
            if (server == null) {
                return SendingResult.CLOSED;
            }
            final List<?> remotes = server.remoteAddresses();
            if (remotes.isEmpty() || 0 == (sequence.get() & 0xf)) {//FIXME make frequency configurable
                poll();//allow clients to knock on the door
                if (remotes.isEmpty()) {
                    return SendingResult.DISCONNECTED;
                }
            }
            final SendingResult result = super.sendMessage(buffer, offset, length);
            if (result == SendingResult.SENT) {
                sequence.increment();
                return SendingResult.SENT;
            }
            return result;
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UdpServerSender{" + bindAddress + '}');
        }
    }
}
