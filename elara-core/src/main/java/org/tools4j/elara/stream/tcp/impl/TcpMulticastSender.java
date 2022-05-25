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
import java.util.ArrayList;
import java.util.List;

final class TcpMulticastSender extends AbstractTcpSender {

    private final List<TcpSender> senders = new ArrayList<>();

    TcpMulticastSender() {
        super();
    }

    void add(final TcpSender sender) {

        senders.add(sender);
    }

    void removeAll() {
        senders.clear();
    }

    @Override
    public boolean isConnected() {
        return !senders.isEmpty();
    }

    @Override
    protected void write(final ByteBuffer buffer) throws IOException {
        int size = senders.size();
        for (int i = 0; i < size; ) {
            if (write(buffer, senders.get(i))) {
                i++;
            } else {
                senders.remove(i);
                size--;
            }
        }
    }

    private boolean write(final ByteBuffer buffer, final TcpSender sender) {
        try {
            sender.write(buffer);
            return true;
        } catch (final IOException e) {
            return false;
        }
    }
}
