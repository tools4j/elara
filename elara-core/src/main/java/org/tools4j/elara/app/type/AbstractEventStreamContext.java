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

import org.tools4j.elara.app.config.EventStreamContext;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Poller;
import org.tools4j.elara.stream.MessageStream;
import org.tools4j.elara.stream.StorePollingMessageStream;

import static java.util.Objects.requireNonNull;

abstract class AbstractEventStreamContext<T extends AbstractEventStreamContext<T>> extends AbstractAppContext<T> implements EventStreamContext {

    private MessageStream eventStream;
    private EventProcessor eventProcessor;

    @Override
    public MessageStream eventStream() {
        return eventStream;
    }

    @Override
    public T eventStream(final MessageStore eventStore) {
        return eventStream(new StorePollingMessageStream(eventStore));
    }

    @Override
    public T eventStream(final Poller eventStorePoller) {
        return eventStream(new StorePollingMessageStream(eventStorePoller));
    }

    @Override
    public T eventStream(final MessageStream eventStream) {
        this.eventStream = requireNonNull(eventStream);
        return self();
    }

    @Override
    public EventProcessor eventProcessor() {
        return eventProcessor;
    }

    @Override
    public T eventProcessor(final EventProcessor eventProcessor) {
        this.eventProcessor = requireNonNull(eventProcessor);
        return self();
    }

    @Override
    public void validate() {
        if (eventStream() == null) {
            throw new IllegalArgumentException("Event stream must be set");
        }
        if (eventProcessor() == null) {
            throw new IllegalArgumentException("Event processor must be set");
        }
        super.validate();
    }
}
