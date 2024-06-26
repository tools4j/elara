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
package org.tools4j.elara.samples.network;

import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.CommandSender.SendingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.samples.network.Buffer.CONSUMED_NOTHING;

public class BufferInputPoller implements InputPoller {

    private final Buffer buffer;

    public BufferInputPoller(final Buffer buffer) {
        this.buffer = requireNonNull(buffer);
    }

    public Buffer buffer() {
        return buffer;
    }

    @Override
    public int poll(final CommandContext commandContext, final CommandSender commandSender) {
        try (final SendingContext context = commandSender.sendingCommand()) {
            final int consumed = buffer.consume(context.buffer(), 0);
            if (consumed == CONSUMED_NOTHING) {
                context.abort();
                return 0;
            }
            context.send(consumed);
            return 1;
        }
    }
}
