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
package org.tools4j.elara.stream.nio;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.stream.nio.SelectionHandler.OK;

public class ReadHandler {

    private final ChannelBufferSupplier channelBufferSupplier;
    private final SelectionHandler baseHandler;
    private final SelectionHandler readHandler = this::onSelectionKey;
    private final ChannelPoller channelPoller = this::pollChannel;
    private final MutableDirectBuffer payload = new UnsafeBuffer(0, 0);

    private NioHeader header;
    private Handler messageHandler = message -> {};

    public ReadHandler(final ChannelBufferSupplier channelBufferSupplier) {
        this(channelBufferSupplier, SelectionHandler.NO_OP);
    }

    public ReadHandler(final ChannelBufferSupplier channelBufferSupplier, final SelectionHandler baseHandler) {
        this.channelBufferSupplier = requireNonNull(channelBufferSupplier);
        this.baseHandler = requireNonNull(baseHandler);
    }

    private void init(final NioHeader header, final Handler messageHandler) {
        if (this.header != header) {
            this.header = requireNonNull(header);
        }
        if (this.messageHandler != messageHandler) {
            this.messageHandler = requireNonNull(messageHandler);
        }
    }
    public SelectionHandler initForSelection(final NioHeader header, final Handler messageHandler) {
        init(header, messageHandler);
        return readHandler;
    }

    public ChannelPoller initForPolling(final NioHeader header, final Handler messageHandler) {
        init(header, messageHandler);
        return channelPoller;
    }

    private int onSelectionKey(final SelectionKey key) throws IOException {
        final int result = baseHandler.onSelectionKey(key);
        if (result != OK) {
            return result;
        }
        if (!key.isReadable()) {
            return OK;
        }
        return pollChannel(key.channel());
    }

    private int pollChannel(final Channel channel) throws IOException {
        final ByteBuffer buffer = channelBufferSupplier.bufferFor(channel);
        readFromChannel(channel, buffer);
        return handleMessages(buffer);
    }

    protected int readFromChannel(final Channel channel, final ByteBuffer buffer) throws IOException {
        return ((ByteChannel)channel).read(buffer);
    }

    private int handleMessages(final ByteBuffer buffer) {
        final int headerLength = header.headerLength();
        final int available = buffer.position();
        int offset = 0;
        while (available - offset >= headerLength) {
            header.wrap(buffer, offset);
            final int payloadLength = header.payloadLength();
            if (available - headerLength - offset < payloadLength) {
                break;
            }
            offset += headerLength;
            handleMessage(header, buffer, offset, payloadLength);
            offset += payloadLength;
        }
        final int remaining = available - offset;
        if (remaining > 0) {
            if (offset > 0) {
                payload.wrap(buffer, 0, buffer.capacity());
                payload.putBytes(0, buffer, offset, remaining);
                payload.wrap(0, 0);
            }
        }
        buffer.limit(buffer.capacity());
        buffer.position(remaining);
        return OK;
    }

    //PRECONDITION: header is wrapped
    private void handleMessage(final NioHeader header, final ByteBuffer buffer, final int offset, final int payloadLength) {
        payload.wrap(buffer, offset, payloadLength);
        try {
            handleMessage(header, payload);
        } catch (final Exception e) {
            //FIXME handle exception
        } finally {
            payload.wrap(0, 0);
            header.unwrap();
        }
    }

    protected void handleMessage(final NioHeader header, final DirectBuffer payload) {
        messageHandler.onMessage(payload);
    }
}