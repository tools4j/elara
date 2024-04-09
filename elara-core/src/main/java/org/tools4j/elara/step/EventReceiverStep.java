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
package org.tools4j.elara.step;

import org.agrona.DirectBuffer;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.stream.MessageReceiver;

import static java.util.Objects.requireNonNull;

/**
 * Polls events from a message receiver and invokes the event handler.
 */
public class EventReceiverStep implements AgentStep {

    private final MessageReceiver eventReceiver;
    private final EventHandler eventHandler;

    private final MessageReceiver.Handler pollerHandler = this::onEvent;

    private final FlyweightEvent flyweightEvent = new FlyweightEvent();

    public EventReceiverStep(final MessageReceiver eventReceiver,
                             final EventHandler eventHandler) {
        this.eventReceiver = requireNonNull(eventReceiver);
        this.eventHandler = requireNonNull(eventHandler);
    }

    @Override
    public int doWork() {
        return eventReceiver.poll(pollerHandler);
    }

    private void onEvent(final DirectBuffer event) {
        eventHandler.onEvent(flyweightEvent.wrap(event, 0));
    }

}
