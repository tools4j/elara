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
package org.tools4j.elara.app.type;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.app.config.EventProcessorConfigurator;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.StoreAppendingMessageSender;
import org.tools4j.elara.stream.MessageSender;

import static java.util.Objects.requireNonNull;

final class FeedbackAppConfiguratorImpl extends AbstractEventReceiverConfigurator<FeedbackAppConfiguratorImpl> implements EventProcessorConfigurator, FeedbackAppConfigurator {

    private int processorSourceId;
    private MessageSender commandSender;
    private EventProcessor eventProcessor;

    @Override
    protected FeedbackAppConfiguratorImpl self() {
        return this;
    }

    @Override
    public int processorSourceId() {
        return processorSourceId;
    }

    @Override
    public EventProcessorConfigurator processorSourceId(final int sourceId) {
        this.processorSourceId = sourceId;
        return this;
    }

    @Override
    public MessageSender commandSender() {
        return commandSender;
    }

    @Override
    public FeedbackAppConfigurator commandStore(final MessageStore commandStore) {
        return commandSender(new StoreAppendingMessageSender(commandStore));
    }

    @Override
    public FeedbackAppConfigurator commandSender(final MessageSender commandStream) {
        this.commandSender = requireNonNull(commandStream);
        return this;
    }

    @Override
    public EventProcessor eventProcessor() {
        return eventProcessor;
    }

    @Override
    public FeedbackAppConfigurator eventProcessor(final EventProcessor eventProcessor) {
        this.eventProcessor = requireNonNull(eventProcessor);
        return this;
    }

    @Override
    public FeedbackAppConfiguratorImpl populateDefaults() {
        return super.populateDefaults();
    }

    @Override
    public FeedbackAppConfigurator populateDefaults(final FeedbackApp app) {
        return this
                .eventProcessor(app)
                .populateDefaults();
    }

    @Override
    public void validate() {
        if (processorSourceId <= 0) {
            throw new IllegalStateException("Processor source ID must be set and positive");
        }
        if (commandSender() == null) {
            throw new IllegalStateException("Command sender or command store must be set");
        }
        if (eventProcessor() == null) {
            throw new IllegalStateException("Event processor must be set");
        }
        super.validate();
    }

    @Override
    public Agent createAgent() {
        populateDefaults().validate();
        throw new IllegalArgumentException("Implement feedback agent");//FIXME implement feedback agent
    }
}
