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
package org.tools4j.elara.aeron;

import io.aeron.Aeron.Context;
import io.aeron.Publication;
import io.aeron.Subscription;

import static java.util.Objects.requireNonNull;

public class Aeron implements AutoCloseable {

    private final io.aeron.Aeron aeron;
    private final AeronConfig config;

    public Aeron(final io.aeron.Aeron aeron, final AeronConfig config) {
        this.aeron = requireNonNull(aeron);
        this.config = requireNonNull(config);
    }

    public static Aeron connect() {
        return connect(AeronConfig.configure());
    }

    public static Aeron connect(final AeronConfig config) {
        return new Aeron(io.aeron.Aeron.connect(), config);
    }

    public static Aeron connect(final Context context) {
        return connect(context, AeronConfig.configure());
    }

    public static Aeron connect(final Context context, final AeronConfig config) {
        return new Aeron(io.aeron.Aeron.connect(context), config);
    }

    public io.aeron.Aeron aeron() {
        return aeron;
    }

    public AeronConfig config() {
        return config;
    }

    public AeronSender openIpcSender(final int streamId) {
        return openSender("aeron:ipc", streamId);
    }

    public AeronSender openSender(final String channel, final int streamId) {
        return senderFor(aeron.addPublication(channel, streamId));
    }

    public AeronSender openExclusiveIpcSender(final int streamId) {
        return openExclusiveSender("aeron:ipc", streamId);
    }

    public AeronSender openExclusiveSender(final String channel, final int streamId) {
        return senderFor(aeron.addExclusivePublication(channel, streamId));
    }

    public AeronSender senderFor(final Publication publication) {
        return new AeronSender(publication, config);
    }

    public AeronReceiver openIpcReceiver(final int streamId) {
        return openReceiver("aeron:ipc", streamId);
    }

    public AeronReceiver openReceiver(final String channel, final int streamId) {
        return receiverFor(aeron.addSubscription(channel, streamId));
    }

    public AeronReceiver receiverFor(final Subscription subscription) {
        return new AeronReceiver(subscription, config);
    }

    public boolean isClosed() {
        return aeron.isClosed();
    }

    @Override
    public void close() {
        aeron.close();
    }
}
