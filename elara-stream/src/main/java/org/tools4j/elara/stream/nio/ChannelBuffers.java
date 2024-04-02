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
package org.tools4j.elara.stream.nio;

import org.agrona.collections.Object2ObjectHashMap;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

public class ChannelBuffers implements ChannelBufferSupplier {

    private final Supplier<? extends ByteBuffer> bufferFactory;
    private final Map<Channel, ByteBuffer> buffers;

    public ChannelBuffers(final int channels, final int bufferCapacity) {
        this(channels, () -> ByteBuffer.allocateDirect(bufferCapacity));
    }

    public ChannelBuffers(final int channels, final Supplier<? extends ByteBuffer> bufferFactory) {
        this.bufferFactory = requireNonNull(bufferFactory);
        this.buffers = new Object2ObjectHashMap<>(channels, DEFAULT_LOAD_FACTOR);
    }

    @Override
    public ByteBuffer bufferFor(final Channel channel) {
        ByteBuffer buffer = buffers.get(channel);
        if (buffer == null) {
            buffers.put(channel, buffer = bufferFactory.get());
        }
        return buffer;
    }

    public void remove(final Channel channel) {
        buffers.remove(channel);
    }

    public void clear() {
        buffers.clear();
    }
}
