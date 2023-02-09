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
package org.tools4j.elara.stream.tcp;

import org.tools4j.elara.stream.tcp.config.TcpClientConfiguration;
import org.tools4j.elara.stream.tcp.config.TcpConfiguration;
import org.tools4j.elara.stream.tcp.config.TcpServerConfiguration;
import org.tools4j.elara.stream.tcp.impl.TcpClient;
import org.tools4j.elara.stream.tcp.impl.TcpServer;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public enum Tcp {
    ;
    public static TcpConnection bind(final String address, final int port) {
        return bind(new InetSocketAddress(address, port));
    }

    public static TcpConnection bind(final SocketAddress address) {
        return bind(address, TcpConfiguration.configureServer().populateDefaults());
    }

    public static TcpConnection bind(final SocketAddress address, final TcpServerConfiguration configuration) {
        return new TcpServer(address, configuration);
    }

    public static TcpConnection connect(final String address, final int port) {
        return connect(new InetSocketAddress(address, port));
    }

    public static TcpConnection connect(final SocketAddress address) {
        return connect(address, TcpConfiguration.configureClient().populateDefaults());
    }

    public static TcpConnection connect(final SocketAddress address, final TcpClientConfiguration configuration) {
        return new TcpClient(address, configuration, configuration.bufferCapacity());
    }

}
