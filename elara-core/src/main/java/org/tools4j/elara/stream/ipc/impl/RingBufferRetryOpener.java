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

import org.agrona.IoUtil;
import org.agrona.concurrent.ringbuffer.RingBuffer;
import org.tools4j.elara.stream.ipc.IpcConfiguration;

import java.io.File;
import java.util.function.Supplier;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.util.Objects.requireNonNull;

public class RingBufferRetryOpener implements Supplier<RingBuffer>, AutoCloseable {

    private final File file;
    private final IpcConfiguration config;
    private final String description;
    private RingBuffer ringBuffer;
    private boolean closed;

    public RingBufferRetryOpener(final File file, final IpcConfiguration config) {
        this.file = requireNonNull(file);
        this.config = requireNonNull(config);
        this.description = file.getAbsolutePath();
    }

    @Override
    public RingBuffer get() {
        if (ringBuffer != null) {
            return ringBuffer;
        }
        if (closed || !file.exists()) {
            return null;
        }
        ringBuffer = RingBuffers.create(IoUtil.mapExistingFile(file, READ_WRITE, description), config);
        return ringBuffer;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        ringBuffer = null;
        closed = true;
    }
}
