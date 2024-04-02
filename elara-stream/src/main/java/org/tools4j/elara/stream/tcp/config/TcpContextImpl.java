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
package org.tools4j.elara.stream.tcp.config;

import org.tools4j.elara.stream.tcp.AcceptListener;
import org.tools4j.elara.stream.tcp.ConnectListener;
import org.tools4j.elara.stream.tcp.TcpSendingStrategy;

import static java.util.Objects.requireNonNull;

class TcpContextImpl implements TcpContext {

    private static final int MIN_BUFFER_CAPACITY = 64;
    private static final int DEFAULT_BUFFER_CAPACITY = 1 << 14;

    private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;
    private long reconnectTimeoutMillis = 10_000;
    protected ConnectListener connectListener = null;
    protected AcceptListener acceptListener = null;
    private TcpSendingStrategy.Factory sendingStrategyFactory = TcpSendingStrategy.MULTICAST;

    @Override
    public int bufferCapacity() {
        return bufferCapacity;
    }

    @Override
    public TcpContextImpl bufferCapacity(final int capacity) {
        if (capacity < MIN_BUFFER_CAPACITY) {
            throw new IllegalArgumentException("Buffer capacity must be at least " + MIN_BUFFER_CAPACITY +
                    " but was " + capacity);
        }
        this.bufferCapacity = capacity;
        return this;
    }

    @Override
    public long reconnectTimeoutMillis() {
        return reconnectTimeoutMillis;
    }

    @Override
    public TcpContext reconnectTimeoutMillis(final long timeoutMillis) {
        this.reconnectTimeoutMillis = timeoutMillis;
        return this;
    }

    @Override
    public ConnectListener connectListener() {
        return connectListener;
    }

    @Override
    public TcpContext connectListener(final ConnectListener listener) {
        this.connectListener = listener;
        return this;
    }

    @Override
    public TcpSendingStrategy.Factory sendingStrategyFactory() {
        return sendingStrategyFactory;
    }

    @Override
    public TcpContext sendingStrategyFactory(final TcpSendingStrategy.Factory factory) {
        this.sendingStrategyFactory = requireNonNull(factory);
        return this;
    }

    @Override
    public AcceptListener acceptListener() {
        return acceptListener;
    }

    @Override
    public TcpContext acceptListener(final AcceptListener listener) {
        this.acceptListener = listener;
        return this;
    }

    @Override
    public void validate() {
        if (connectListener == null) {
            throw new IllegalArgumentException("Connect listener must be set (hint: use populateDefaults())");
        }
        if (acceptListener == null) {
            throw new IllegalArgumentException("Accept listener must be set (hint: use populateDefaults())");
        }
    }

    @Override
    public TcpContext populateDefaults() {
        if (connectListener == null) {
            connectListener = defaultConnectListener();
        }
        if (acceptListener == null) {
            acceptListener = defaultAcceptListener();
        }
        return this;
    }

    static final class TcpClientContextImpl extends TcpContextImpl {
        @Override
        public void validate() {
            if (connectListener == null) {
                throw new IllegalArgumentException("Connect listener must be set (hint: use populateDefaults())");
            }
        }

        @Override
        public TcpContext populateDefaults() {
            if (connectListener == null) {
                connectListener = defaultConnectListener();
            }
            return this;
        }
    }

    static final class TcpServerContextImpl extends TcpContextImpl {
        @Override
        public void validate() {
            if (acceptListener == null) {
                throw new IllegalArgumentException("Accept listener must be set (hint: use populateDefaults())");
            }
        }

        @Override
        public TcpContext populateDefaults() {
           if (acceptListener == null) {
                acceptListener = defaultAcceptListener();
            }
            return this;
        }
    }

    private static ConnectListener defaultConnectListener() {
        return (client, channel, key) ->
                System.out.printf("%s: connected to %s\n", client, channel.socket().getRemoteSocketAddress());
    }

    private static AcceptListener defaultAcceptListener() {
        return (server, serverChannel, clientChannel, key) ->
                System.out.printf("%s: accepted from %s\n", server, clientChannel.socket().getRemoteSocketAddress());
    }
}
