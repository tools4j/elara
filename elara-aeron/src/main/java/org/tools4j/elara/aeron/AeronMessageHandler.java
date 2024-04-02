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
package org.tools4j.elara.aeron;

import io.aeron.FragmentAssembler;
import io.aeron.logbuffer.FragmentHandler;
import io.aeron.logbuffer.Header;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageReceiver.Handler;

final class AeronMessageHandler {
    private final DirectBuffer message = new UnsafeBuffer(0, 0);
    private final FragmentHandler fragmentHandler;
    private Header fragmentHeader;
    private Handler handler;

    AeronMessageHandler(final AeronConfig config, final boolean useFragmentAssembler) {
        this.fragmentHandler = useFragmentAssembler ?
                new FragmentAssembler(this::onFragment, config.receiverFragmentAssemblerInitialBufferSize(), true) :
                this::onFragment;

    }

    FragmentHandler init(final Handler handler) {
        if (this.handler != handler) {
            this.handler = handler;
        }
        return fragmentHandler;
    }

    Header fragmentHeader() {
        return fragmentHeader;
    }

    private void onFragment(final DirectBuffer buffer, final int offset, final int length, final Header header) {
        try {
            message.wrap(buffer, offset, length);
            fragmentHeader = header;
            handler.onMessage(message);
        } finally {
            fragmentHeader = null;
            message.wrap(0, 0);
        }
    }
}
