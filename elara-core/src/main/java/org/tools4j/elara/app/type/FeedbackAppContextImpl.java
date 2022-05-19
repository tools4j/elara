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
package org.tools4j.elara.app.type;

import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.stream.MessageSender;
import org.tools4j.elara.stream.StoreAppendingMessageSender;

import static java.util.Objects.requireNonNull;

final class FeedbackAppContextImpl extends AbstractEventStreamContext<FeedbackAppContextImpl> implements FeedbackAppContext {

    public static final String DEFAULT_THREAD_NAME = "elara-feed";

    private MessageSender commandStream;

    @Override
    protected FeedbackAppContextImpl self() {
        return this;
    }

    @Override
    public MessageSender commandStream() {
        return commandStream;
    }

    @Override
    public FeedbackAppContext commandStream(final MessageStore commandStore) {
        return commandStream(new StoreAppendingMessageSender(commandStore));
    }

    @Override
    public FeedbackAppContext commandStream(final MessageSender commandStream) {
        this.commandStream = requireNonNull(commandStream);
        return this;
    }

    @Override
    public FeedbackAppContext populateDefaults() {
        return populateDefaults(DEFAULT_THREAD_NAME);
    }

    @Override
    public FeedbackAppContext populateDefaults(final FeedbackApp app) {
        return this
                .eventProcessor(app)
                .populateDefaults();
    }

    @Override
    public void validate() {
        if (commandStream() == null) {
            throw new IllegalArgumentException("Command stream must be set");
        }
        super.validate();
    }
}
