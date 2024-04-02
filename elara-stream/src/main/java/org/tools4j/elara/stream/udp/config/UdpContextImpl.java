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
package org.tools4j.elara.stream.udp.config;

import org.tools4j.elara.stream.udp.RemoteAddressListener;
import org.tools4j.elara.stream.udp.UdpSendingStrategy;

import static java.util.Objects.requireNonNull;

class UdpContextImpl implements UdpContext {

    private static final int MIN_BUFFER_CAPACITY = 64;
    private static final int DEFAULT_BUFFER_CAPACITY = 1 << 14;
    private static final int MIN_MTU_LENGTH = 64;
    private static final int DEFAULT_MTU_LENGTH = 1400;

    private int bufferCapacity = DEFAULT_BUFFER_CAPACITY;
    private RemoteAddressListener remoteAddressListener = null;
    private UdpSendingStrategy.Factory sendingStrategyFactory = UdpSendingStrategy.MULTICAST;
    private int mtuLength = DEFAULT_MTU_LENGTH;

    @Override
    public int bufferCapacity() {
        return bufferCapacity;
    }

    @Override
    public UdpContext bufferCapacity(final int capacity) {
        if (capacity < MIN_BUFFER_CAPACITY) {
            throw new IllegalArgumentException("Buffer capacity must be at least " + MIN_BUFFER_CAPACITY +
                    " but was " + capacity);
        }
        this.bufferCapacity = capacity;
        return this;
    }

    @Override
    public UdpSendingStrategy.Factory sendingStrategyFactory() {
        return sendingStrategyFactory;
    }

    @Override
    public UdpContext sendingStrategyFactory(final UdpSendingStrategy.Factory factory) {
        this.sendingStrategyFactory = requireNonNull(factory);
        return this;
    }

    @Override
    public RemoteAddressListener remoteAddressListener() {
        return remoteAddressListener;
    }

    @Override
    public UdpContext remoteAddressListener(final RemoteAddressListener listener) {
        this.remoteAddressListener = listener;
        return this;
    }

    @Override
    public int mtuLength() {
        return mtuLength;
    }

    @Override
    public UdpContext mtuLength(final int mtuLength) {
        if (mtuLength < MIN_MTU_LENGTH) {
            throw new IllegalArgumentException("MTU length must be at least " + MIN_MTU_LENGTH +
                    " but was " + mtuLength);
        }
        this.mtuLength = mtuLength;
        return this;
    }

    @Override
    public void validate() {
        if (remoteAddressListener == null) {
            throw new IllegalArgumentException("Remote address listener must be set (hint: use populateDefaults())");
        }
    }

    @Override
    public UdpContext populateDefaults() {
        if (remoteAddressListener == null) {
            remoteAddressListener = defaultRemoteAddressListener();
        }
        return this;
    }

    static final class UdpClientContextImpl extends UdpContextImpl implements UdpClientContext {
        @Override
        public void validate() {
            //nothing to validate
        }

        @Override
        public UdpContext populateDefaults() {
            //nothing to populate
            return this;
        }
    }

    static final class UdpServerContextImpl extends UdpContextImpl implements UdpServerContext {
        //nothing to add
    }

    private static RemoteAddressListener defaultRemoteAddressListener() {
        return (server, serverChannel, remoteAddress) ->
                System.out.printf("%s: added remote address %s\n", server, remoteAddress);
    }
}
