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

final class AeronContextImpl implements AeronContext {
    private int senderInitialBufferSize = 4096;
    private int senderMaxRetriesAfterAdminAction = 3;
    private int receiverFragmentLimit = 1;
    private int receiverFragmentAssemblerInitialBufferSize = 4096;

    @Override
    public int senderInitialBufferSize() {
        return senderInitialBufferSize;
    }

    @Override
    public int senderMaxRetriesAfterAdminAction() {
        return senderMaxRetriesAfterAdminAction;
    }

    @Override
    public int receiverFragmentLimit() {
        return receiverFragmentLimit;
    }

    @Override
    public int receiverFragmentAssemblerInitialBufferSize() {
        return receiverFragmentAssemblerInitialBufferSize;
    }

    @Override
    public AeronContext senderInitialBufferSize(final int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Initial sender buffer size cannot be negative: " + bytes);
        }
        this.senderInitialBufferSize = bytes;
        return this;
    }

    @Override
    public AeronContext senderMaxRetriesAfterAdminAction(final int maxRetries) {
        this.senderMaxRetriesAfterAdminAction = maxRetries;
        return this;
    }

    @Override
    public AeronContext receiverFragmentLimit(final int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Receiver fragment limit cannot be less than one: " + limit);
        }
        this.receiverFragmentLimit = limit;
        return this;
    }

    @Override
    public AeronContext receiverFragmentAssemblerInitialBufferSize(final int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Initial receiver fragment assembler buffer size cannot be negative: " + bytes);
        }
        this.receiverFragmentAssemblerInitialBufferSize = bytes;
        return this;
    }

    @Override
    public String toString() {
        return "AeronContextImpl:" +
                "senderInitialBufferSize=" + senderInitialBufferSize +
                "|senderMaxRetriesAfterAdminAction=" + senderMaxRetriesAfterAdminAction +
                "|receiverFragmentLimit=" + receiverFragmentLimit +
                "|receiverFragmentAssemblerInitialBufferSize=" + receiverFragmentAssemblerInitialBufferSize;
    }
}
