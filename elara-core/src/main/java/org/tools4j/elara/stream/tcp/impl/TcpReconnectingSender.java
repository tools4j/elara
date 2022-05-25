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

import java.io.IOException;
import java.nio.ByteBuffer;

import static java.util.Objects.requireNonNull;

final class TcpReconnectingSender extends AbstractTcpSender {

    private static final DisconnectedSender DISCONNECTED = new DisconnectedSender("TcpSender is not yet connected");
    private static final DisconnectedSender FAILED = new DisconnectedSender("TcpSender is not connected");
    private static final DisconnectedSender CLOSED = new DisconnectedSender("TcpSender is closed");

    private AbstractTcpSender sender = DISCONNECTED;

    TcpReconnectingSender() {
        super();
    }

    void init() {
        if (sender != CLOSED) {
            this.sender = DISCONNECTED;
        }
    }

    void close() {
        this.sender = CLOSED;
    }

    void connect(final TcpSender sender) {
        if (this.sender != CLOSED) {
            this.sender = requireNonNull(sender);
        }
    }

    @Override
    public boolean isConnected() {
        return sender.isConnected();
    }

    boolean isFailed() {
        return sender == FAILED;
    }

    boolean isRealSender() {
        return !(sender instanceof DisconnectedSender);
    }

    @Override
    protected void write(final ByteBuffer buffer) throws IOException {
        try {
            sender.write(buffer);
        } catch (final IOException e) {
            if (isRealSender()) {
                sender = FAILED;
            }
        }
    }

    private static final class DisconnectedSender extends AbstractTcpSender {

        final String message;

        DisconnectedSender(final String message) {
            this.message = requireNonNull(message);
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        protected void write(final ByteBuffer buffer) throws IOException{
            throw new IOException(message);
        }
    }
}
