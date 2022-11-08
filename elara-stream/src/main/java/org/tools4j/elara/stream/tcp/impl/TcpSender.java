/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.stream.tcp.impl;

import org.agrona.DirectBuffer;
import org.agrona.LangUtil;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.stream.tcp.TcpConnection;
import org.tools4j.elara.stream.tcp.impl.TcpPoller.SelectionHandler;

import java.net.ConnectException;
import java.nio.channels.SelectionKey;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

class TcpSender extends  MessageSender.Buffered {

    private final TcpConnection connection;
    private final Supplier<? extends TcpEndpoint> endpointSupplier;

    TcpSender(final TcpConnection connection, final Supplier<? extends TcpEndpoint> endpointSupplier) {
        super(4096);//FIXME configure
        this.connection = requireNonNull(connection);
        this.endpointSupplier = requireNonNull(endpointSupplier);
    }

    @Override
    public SendingResult sendMessage(final DirectBuffer buffer, final int offset, final int length) {
        final TcpEndpoint endpoint = endpointSupplier.get();
        if (endpoint == null) {
            return SendingResult.CLOSED;
        }
        try {
            final int readyOps = endpoint.send(buffer, offset, length);
            if (readyOps > 0 && ((readyOps & SelectionKey.OP_WRITE) != 0)) {
                return SendingResult.SENT;
            }
            if (readyOps < 0) {
                if ((readyOps & SelectionHandler.MESSAGE_TOO_LARGE) != 0) {
                    //FIXME log
                    return SendingResult.FAILED;
                }
                if ((readyOps & SelectionHandler.BACK_PRESSURE) != 0) {
                    return SendingResult.BACK_PRESSURED;
                }
            }
            return connection.isConnected() ? SendingResult.BACK_PRESSURED : SendingResult.DISCONNECTED;
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
        final TcpEndpoint endpoint = endpointSupplier.get();
        if (endpoint != null) {
            endpoint.close();
        }
    }
}
