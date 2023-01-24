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
package org.tools4j.elara.app.type;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.StoreAppendingMessageSender;
import org.tools4j.elara.stream.MessageSender;

import static java.util.Objects.requireNonNull;

final class FeedbackAppContextImpl extends AbstractEventStreamContext<FeedbackAppContextImpl> implements FeedbackAppContext {

    private MessageSender commandSender;

    @Override
    protected FeedbackAppContextImpl self() {
        return this;
    }

    @Override
    public MessageSender commandSender() {
        return commandSender;
    }

    @Override
    public FeedbackAppContext commandStore(final MessageStore commandStore) {
        return commandSender(new StoreAppendingMessageSender(commandStore));
    }

    @Override
    public FeedbackAppContext commandSender(final MessageSender commandStream) {
        this.commandSender = requireNonNull(commandStream);
        return this;
    }

    @Override
    public FeedbackAppContextImpl populateDefaults() {
        return super.populateDefaults();
    }

    @Override
    public FeedbackAppContext populateDefaults(final FeedbackApp app) {
        return this
                .eventProcessor(app)
                .populateDefaults();
    }

    @Override
    public void validate() {
        if (commandSender() == null) {
            throw new IllegalArgumentException("Command sender or command store must be set");
        }
        super.validate();
    }

    @Override
    public Agent createAgent() {
        populateDefaults().validate();
        throw new IllegalArgumentException("Implement feedback agent");//FIXME implement feedback agent
    }
}
