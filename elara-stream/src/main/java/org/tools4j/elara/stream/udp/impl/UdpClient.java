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
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.nio.BiDirectional;
import org.tools4j.elara.stream.nio.NioReceiver;
import org.tools4j.elara.stream.nio.NioSender;
import org.tools4j.elara.stream.nio.RingBuffer;

import java.net.SocketAddress;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class UdpClient implements BiDirectional {

    private final SocketAddress connectAddress;
    private final Supplier<? extends RingBuffer> ringBufferFactory;
    private final UcpClientReceiver receiver = new UcpClientReceiver();
    private final UcpClientSender sender = new UcpClientSender();
    private UdpClientEndpoint client;

    public UdpClient(final SocketAddress connectAddress, final int bufferCapacity) {
        this.connectAddress = requireNonNull(connectAddress);
        this.ringBufferFactory = RingBuffer.factory(bufferCapacity);
        this.client = new UdpClientEndpoint(connectAddress, ringBufferFactory);
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
            client = new UdpClientEndpoint(connectAddress, ringBufferFactory);
        }
    }

    @Override
    public String toString() {
        return "UdpClient{" + connectAddress + '}';
    }

    private final class UcpClientReceiver extends NioReceiver {
        String name;
        UcpClientReceiver() {
            super(() -> client);
        }

        @Override
        public int poll(final Handler handler) {
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

    private final class UcpClientSender extends NioSender {
        String name;
        UcpClientSender() {
            super(() -> client);
        }

        @Override
        public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
            final SendingResult result = super.sendMessage(buffer, offset, length);
            if (result != SendingResult.SENT) {
                if (client != null && client.isClosed()) {
                    reconnect();
                }
            }
            return result;
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UcpClientSender{" + connectAddress + '}');
        }
    }

}
