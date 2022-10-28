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
package org.tools4j.elara.stream.ipc.impl;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.MessageHandler;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageReceiver.Handler;

import static org.tools4j.elara.stream.ipc.impl.RingBuffers.LENGTH_PREFIXED_MSG_TYPE_ID;
import static org.tools4j.elara.stream.ipc.impl.RingBuffers.PLAIN_MSG_TYPE_ID;

final class IpcMessageHandler implements MessageHandler {

    private final DirectBuffer message = new UnsafeBuffer(0, 0);
    private Handler handler;

    @Override
    public void onMessage(final int msgTypeId, final MutableDirectBuffer buffer, final int offset, final int length) {
        if (msgTypeId == PLAIN_MSG_TYPE_ID) {
            message.wrap(buffer, offset, length);
        } else if (msgTypeId == LENGTH_PREFIXED_MSG_TYPE_ID) {
            final int messageLength = buffer.getInt(offset);
            message.wrap(buffer, offset + Integer.BYTES, messageLength);
        } else {
            throw new IllegalArgumentException("Unsupported msgTypeId: " + msgTypeId);
        }
        handler.onMessage(message);
        message.wrap(0, 0);
    }

    IpcMessageHandler init(final Handler handler) {
        if (this.handler != handler) {
            this.handler = handler;
        }
        return this;
    }
}
