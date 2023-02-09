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
import org.agrona.LangUtil;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.stream.BufferingSendingContext;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.nio.NioHeader.MutableNioHeader;

import java.net.ConnectException;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class NioSender implements MessageSender.Direct {

    private final Supplier<? extends NioEndpoint> endpointSupplier;
    private final NioFrame frame;

    private final NioSendingContext context;

    public NioSender(final Supplier<? extends NioEndpoint> endpointSupplier,
                     final int bufferSize,
                     final MutableNioHeader header) {
        this.endpointSupplier = requireNonNull(endpointSupplier);
        this.frame = new NioFrameImpl(header, bufferSize);
        this.context = new NioSendingContext(this, frame.payload());
    }

    @Override
    public SendingContext sendingMessage() {
        return context.init();
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        assert offset == 0;
        if (buffer != frame.payload()) {
            frame.payload().putBytes(0, buffer, offset, length);
        }
        writeHeader(frame.header(), length);
        return sendFrame(frame.header().headerLength() + length);
    }

    protected void writeHeader(final MutableNioHeader header, final int payloadLength) {
        header.payloadLength(payloadLength);
    }

    private SendingResult sendFrame(final int frameLength) {
        final NioEndpoint endpoint = endpointSupplier.get();
        if (endpoint == null) {
            return SendingResult.CLOSED;
        }
        try {
            final int readyOps = endpoint.send(frame.frame(), 0, frameLength);
            if (readyOps > 0 && ((readyOps & SelectionKey.OP_WRITE) != 0)) {
                return SendingResult.SENT;
            }
            if (readyOps < 0) {
                if ((readyOps & SelectionHandler.MESSAGE_TOO_LARGE) != 0) {
                    //FIXME log
                    return SendingResult.FAILED;
                }
                if ((readyOps & SelectionHandler.DISCONNECTED) != 0) {
                    return SendingResult.DISCONNECTED;
                }
                if ((readyOps & SelectionHandler.BACK_PRESSURE) != 0) {
                    return SendingResult.BACK_PRESSURED;
                }
            }
            return endpoint.isConnected() ? SendingResult.BACK_PRESSURED : SendingResult.DISCONNECTED;
        } catch (final ConnectException e) {
            return SendingResult.DISCONNECTED;
        } catch (final Exception e) {
            //FIXME log or handle
            LangUtil.rethrowUnchecked(e);
            return SendingResult.FAILED;
        }
    }

    @Override
    public boolean isClosed() {
        return null == endpointSupplier.get();
    }

    @Override
    public void close() {
        final NioEndpoint endpoint = endpointSupplier.get();
        if (endpoint != null) {
            endpoint.close();
        }
    }

    private static class NioSendingContext extends BufferingSendingContext {
        public NioSendingContext(final MessageSender sender, final MutableDirectBuffer buffer) {
            super(sender, buffer);
        }

        @Override
        protected BufferingSendingContext init() {
            return super.init();
        }
    }
}
