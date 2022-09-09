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
package org.tools4j.elara.stream.ipc;

import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.logging.Logger.Factory;

import static java.util.Objects.requireNonNull;

final class IpcContextImpl implements IpcContext {

    private Logger.Factory loggerFactory = Logger.systemLoggerFactory();
    private Cardinality senderCardinality = Cardinality.ONE;
    private int maxMessagesReceivedPerPoll = 1;
    private int retryOpenInterval = 10;

    @Override
    public Factory loggerFactory() {
        return loggerFactory;
    }

    @Override
    public Cardinality senderCardinality() {
        return senderCardinality;
    }

    @Override
    public int maxMessagesReceivedPerPoll() {
        return maxMessagesReceivedPerPoll;
    }

    @Override
    public int retryOpenInterval() {
        return retryOpenInterval;
    }

    @Override
    public IpcContext loggerFactory(final Factory loggerFactory) {
        this.loggerFactory = requireNonNull(loggerFactory);
        return this;
    }

    @Override
    public IpcContext senderCardinality(final Cardinality cardinality) {
        this.senderCardinality = requireNonNull(senderCardinality);
        return this;
    }

    @Override
    public IpcContext maxMessagesReceivedPerPoll(final int maxMessages) {
        if (maxMessages <= 0) {
            throw new IllegalArgumentException("Max messages received per poll must be positive but was " + maxMessages);
        }
        this.maxMessagesReceivedPerPoll = requireNonNull(maxMessages);
        return this;
    }

    @Override
    public IpcContext retryOpenInterval(final int interval) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Retry open interval must be positive but was " + interval);
        }
        this.retryOpenInterval = interval;
        return this;
    }
}
