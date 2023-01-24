/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.stream.ipc;

import static java.util.Objects.requireNonNull;

final class IpcContextImpl implements IpcContext {

    private Cardinality senderCardinality = Cardinality.ONE;
    private int senderInitialBufferSize = 4096;
    private AllocationStrategy allocationStrategy = AllocationStrategy.DYNAMIC;
    private int maxMessagesReceivedPerPoll = 1;
    private boolean newFileCreateParentDirs = true;
    private boolean newFileDeleteIfPresent = false;

    @Override
    public Cardinality senderCardinality() {
        return senderCardinality;
    }

    @Override
    public int senderInitialBufferSize() {
        return senderInitialBufferSize;
    }

    @Override
    public AllocationStrategy senderAllocationStrategy() {
        return allocationStrategy;
    }

    @Override
    public int maxMessagesReceivedPerPoll() {
        return maxMessagesReceivedPerPoll;
    }

    @Override
    public boolean newFileCreateParentDirs() {
        return newFileCreateParentDirs;
    }

    @Override
    public boolean newFileDeleteIfPresent() {
        return newFileDeleteIfPresent;
    }

    @Override
    public IpcContext senderCardinality(final Cardinality cardinality) {
        this.senderCardinality = requireNonNull(cardinality);
        return this;
    }

    @Override
    public IpcContext senderInitialBufferSize(final int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("Initial sender buffer size cannot be negative: " + bytes);
        }
        senderInitialBufferSize = bytes;
        return this;
    }

    @Override
    public IpcContext senderAllocationStrategy(final AllocationStrategy strategy) {
        this.allocationStrategy = requireNonNull(strategy);
        return this;
    }

    @Override
    public IpcContext maxMessagesReceivedPerPoll(final int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("Max messages received per poll must be positive but was " + maxMessages);
        }
        this.maxMessagesReceivedPerPoll = maxMessages;
        return this;
    }

    @Override
    public IpcContext newFileCreateParentDirs(final boolean create) {
        this.newFileCreateParentDirs = create;
        return this;
    }

    @Override
    public IpcContext newFileDeleteIfPresent(final boolean delete) {
        this.newFileDeleteIfPresent = delete;
        return this;
    }

    @Override
    public String toString() {
        return "IpcContext:" +
                "senderCardinality=" + senderCardinality +
                "|senderInitialBufferSize=" + senderInitialBufferSize +
                "|allocationStrategy=" + allocationStrategy +
                "|maxMessagesReceivedPerPoll=" + maxMessagesReceivedPerPoll +
                "|newFileCreateParentDirs=" + newFileCreateParentDirs +
                "|newFileDeleteIfPresent=" + newFileDeleteIfPresent;
    }
}
