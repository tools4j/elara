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

import io.aeron.FragmentAssembler;
import io.aeron.Subscription;
import org.tools4j.elara.stream.MessageReceiver;

import static java.util.Objects.requireNonNull;

public class AeronReceiver implements MessageReceiver {

    private final Subscription subscription;
    private final AeronMessageHandler aeronMessageHandler;
    private final int fragmentLimit;
    private String name;

    public AeronReceiver(final Subscription subscription, final AeronConfig config) {
        this.subscription = requireNonNull(subscription);
        this.aeronMessageHandler = new AeronMessageHandler(fragmentHandler -> new FragmentAssembler(fragmentHandler,
                config.receiverFragmentAssemblerInitialBufferSize(), true));
        this.fragmentLimit = config.receiverFragmentLimit();
    }

    @Override
    public int poll(final Handler handler) {
        return subscription.poll(aeronMessageHandler.init(handler), fragmentLimit);
    }

    @Override
    public boolean isClosed() {
        return subscription.isClosed();
    }

    @Override
    public void close() {
        subscription.close();
    }

    @Override
    public String toString() {
        return name != null ? name : (name = "AeronReceiver{" + subscription.channel() + ":" + subscription.streamId() + '}');
    }
}
