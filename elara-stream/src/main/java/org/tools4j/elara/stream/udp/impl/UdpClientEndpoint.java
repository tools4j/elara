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

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

final class UdpClientEndpoint implements NioEndpoint {

    private final DatagramChannel datagramChannel;
    private final ReadHandler readSelectionHandler;
    private final WriteHandler writeSelectionHandler;
    private final NioPoller receiverPoller = new NioPoller();
    private final NioPoller senderPoller = new NioPoller();

    UdpClientEndpoint(final SocketAddress connectAddress,
                      final int mtuLength,
                      final Supplier<? extends ByteBuffer> bufferFactory) {
        try {
            this.datagramChannel = DatagramChannel.open()
                    .setOption(StandardSocketOptions.SO_REUSEADDR, true);
            this.readSelectionHandler = new ReadHandler(new ChannelBuffers(1, bufferFactory));
            this.writeSelectionHandler = new UdpWriteSelectionHandler(new ChannelBuffers(1, bufferFactory), mtuLength);
            this.datagramChannel.configureBlocking(false);
            this.datagramChannel.register(receiverPoller.selector(), SelectionKey.OP_READ);
            this.datagramChannel.register(senderPoller.selector(), SelectionKey.OP_WRITE);
            this.datagramChannel.connect(connectAddress);
        } catch (final IOException e) {
            LangUtil.rethrowUnchecked(e);
            throw new RuntimeException(e);//we never get here
        }
    }

    @Override
    public void close() {
        try {
            senderPoller.close();
            datagramChannel.close();
        } catch (final Exception ex) {
            LangUtil.rethrowUnchecked(ex);
        }
    }

    @Override
    public int receive(final NioHeader header, final Handler messageHandler) throws IOException {
        return readSelectionHandler.initForPolling(header, messageHandler).poll(datagramChannel);
//        return receiverPoller.selectNow(readSelectionHandler.initForSelection(header, messageHandler));
    }

    @Override
    public int send(final NioFrame frame) throws IOException {
        final int result = writeSelectionHandler.send(datagramChannel, frame);
        return result == 0 ? SelectionKey.OP_WRITE : result;
//        return senderPoller.selectNow(writeSelectionHandler.initForSelection(frame));
    }

    public boolean isConnected() {
        return datagramChannel.isConnected();
    }

    @Override
    public boolean isClosed() {
        return !datagramChannel.isOpen();
    }

    @Override
    public String toString() {
        return "UdpClient{" + datagramChannel.socket().getLocalAddress() + '}';
    }
}