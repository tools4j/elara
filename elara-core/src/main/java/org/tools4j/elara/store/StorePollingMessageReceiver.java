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
package org.tools4j.elara.store;

import org.agrona.DirectBuffer;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.stream.MessageReceiver;

import static java.util.Objects.requireNonNull;

public class StorePollingMessageReceiver implements MessageReceiver {

    private final MessageStore.Poller messageStorePoller;
    private final MessageStore.Handler storeHandler = this::onMessage;
    private Handler receiverHandler;

    public StorePollingMessageReceiver(final MessageStore messageStore) {
        this(messageStore.poller());
    }

    public StorePollingMessageReceiver(final MessageStore.Poller messageStorePoller) {
        this.messageStorePoller = requireNonNull(messageStorePoller);
    }

    @Override
    public String toString() {
        return "StorePollingMessageReceiver";
    }

    @Override
    public int poll(final Handler handler) {
        requireNonNull(handler);
        if (receiverHandler != handler) {
            receiverHandler = handler;
        }
        return messageStorePoller.poll(storeHandler);
    }

    private Result onMessage(final DirectBuffer buffer) {
        receiverHandler.onMessage(buffer);
        return Result.POLL;
    }

    @Override
    public boolean isClosed() {
        return messageStorePoller.isClosed();
    }

    @Override
    public void close() {
        messageStorePoller.close();
    }
}
