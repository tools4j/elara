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

import org.agrona.CloseHelper;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;
import org.tools4j.elara.stream.nio.NioReceiver;
import org.tools4j.elara.stream.nio.NioSender;
import org.tools4j.elara.stream.nio.RingBuffer;
import org.tools4j.elara.stream.udp.UdpEndpoint;
import org.tools4j.elara.stream.udp.UdpHeader;
import org.tools4j.elara.stream.udp.UdpReceiver;
import org.tools4j.elara.stream.udp.UdpSender;
import org.tools4j.elara.stream.udp.config.UdpClientConfiguration;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class UdpClient implements UdpEndpoint {
    private static final UnsafeBuffer HELLO = new UnsafeBuffer(ByteBuffer.allocate(0));
    private final SocketAddress connectAddress;
    private final UdpClientConfiguration configuration;
    private final Supplier<? extends RingBuffer> ringBufferFactory;
    private final UcpClientReceiver receiver;
    private final UcpClientSender sender;
    private UdpClientEndpoint client;

    public UdpClient(final SocketAddress connectAddress, final UdpClientConfiguration configuration) {
        configuration.validate();
        this.connectAddress = requireNonNull(connectAddress);
        this.configuration = requireNonNull(configuration);
        this.ringBufferFactory = RingBuffer.factory(configuration.bufferCapacity());
        this.receiver = new UcpClientReceiver(new MutableUdpHeader());
        this.sender = new UcpClientSender(new MutableUdpHeader());
        this.client = new UdpClientEndpoint(connectAddress, configuration.mtuLength(), ringBufferFactory);
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
    public int poll() {
        return 0;
    }

    @Override
    public boolean isClosed() {
        return client == null;
    }

    @Override
    public void close() {
        if (client != null) {
            CloseHelper.quietClose(client);
            client = null;
        }
    }

    public void reconnect() {
        if (client != null) {
            CloseHelper.quietClose(client);
            client = new UdpClientEndpoint(connectAddress, configuration.mtuLength(), ringBufferFactory);
        }
    }

    @Override
    public String toString() {
        return "UdpClient{" + connectAddress + '}';
    }

    private final class UcpClientReceiver extends NioReceiver implements UdpReceiver {
        final UdpHeader header;
        String name;
        UcpClientReceiver(final UdpHeader header) {
            super(() -> client, header);
            this.header = requireNonNull(header);
        }

        @Override
        public UdpHeader header() {
            return header.buffer().capacity() > 0 ? header : null;
        }

        @Override
        public int poll(final Handler handler) {
            if (sender.sequence() == 0) {
                sender.sendHello();
            }
            try {
                return super.poll(handler);
            } catch (final Exception e) {
                reconnect();
                throw e;
            }
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UcpClientReceiver{" + connectAddress + '}');
        }
    }

    private final class UcpClientSender extends NioSender implements UdpSender {
        final MutableUdpHeader header;
        final Sequence sequence = new Sequence();

        String name;
        UcpClientSender(final MutableUdpHeader header) {
            super(() -> client, configuration.bufferCapacity(), header);
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
            final SendingResult result = super.sendMessage(buffer, offset, length);
            if (result == SendingResult.SENT) {
                sequence.increment();
                return SendingResult.SENT;
            }
            if (client != null && client.isClosed()) {
                reconnect();
            }
            return result;
        }

        void sendHello() {
            sendMessage(HELLO, 0, 0);
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UcpClientSender{" + connectAddress + '}');
        }
    }

}
