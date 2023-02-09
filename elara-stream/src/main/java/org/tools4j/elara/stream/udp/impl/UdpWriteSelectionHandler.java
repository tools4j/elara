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

import org.tools4j.elara.stream.nio.RingBuffer;
import org.tools4j.elara.stream.nio.WriteSelectionHandler;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.util.function.Supplier;

class UdpWriteSelectionHandler extends WriteSelectionHandler {

    private final int mtuLength;

    UdpWriteSelectionHandler(final Supplier<? extends RingBuffer> ringBufferFactory, final int mtuLength) {
        super(ringBufferFactory);
        this.mtuLength = mtuLength;
    }

    @Override
    protected int writeToChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
        final DatagramChannel datagramChannel = (DatagramChannel)channel;
        return sendFragments(datagramChannel, datagramChannel.getRemoteAddress(), buffer);
    }

    protected int sendFragments(final DatagramChannel channel, final SocketAddress target, final ByteBuffer buffer) throws IOException {
        final int position = buffer.position();
        final int limit = buffer.limit();
        if (limit - position <= mtuLength) {
            return channel.send(buffer, target);
        }
        int sent = 0;
        int mtuLimit = position;
        do {
            mtuLimit += mtuLength;
            buffer.limit(Math.min(mtuLimit, limit));
            sent += channel.send(buffer, target);
            if (sent == 0) {
                return 0;
            }
        } while (mtuLimit < limit);
        return sent;
    }
}
