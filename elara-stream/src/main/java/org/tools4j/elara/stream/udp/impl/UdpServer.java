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
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.nio.BiDirectional;
import org.tools4j.elara.stream.nio.NioReceiver;
import org.tools4j.elara.stream.nio.NioSender;
import org.tools4j.elara.stream.nio.RingBuffer;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class UdpServer implements BiDirectional {

    private final SocketAddress bindAddress;
    private final UdpServerSender sender = new UdpServerSender();
    private final UdpServerReceiver receiver = new UdpServerReceiver();;
    private UdpServerEndpoint server;

    public UdpServer(final SocketAddress bindAddress, final int bufferCapacity) {
        this.bindAddress = requireNonNull(bindAddress);
        this.server = new UdpServerEndpoint(bindAddress, RingBuffer.factory(bufferCapacity));
    }

    @Override
    public int poll() {
        return 0;
    }

    @Override
    public MessageSender sender() {
        return sender;
    }

    @Override
    public MessageReceiver receiver() {
        return receiver;
    }

    private final List<SocketChannel> accepted = new ArrayList<>();

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

    private final class UdpServerReceiver extends NioReceiver {
        String name;
        UdpServerReceiver() {
            super(() -> server);
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UdpServerReceiver{" + bindAddress + '}');
        }
    }

    private final class UdpServerSender extends NioSender {
        String name;
        UdpServerSender() {
            super(() -> server);
        }

        @Override
        public String toString() {
            return name != null ? name : (name = "UdpServerSender{" + bindAddress + '}');
        }
    }
}