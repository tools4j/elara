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
import org.tools4j.elara.store.CommittedEventPoller;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Handler;
import org.tools4j.elara.store.MessageStore.Handler.Result;

import static java.util.Objects.requireNonNull;

/**
 * Polls all events and invokes the event handler until reaching the end of the event log.  When the end of the event
 * log is reached, the poller is closed and the step turns into a no-op.
 * @see EventPollerStep
 */
public class EventReplayStep implements AgentStep {

    private final MessageStore.Poller eventPoller;
    private final EventHandler eventHandler;

    private final Handler pollerHandler = this::onEvent;
    private final FlyweightEvent flyweightEvent = new FlyweightEvent();
    private boolean replayed;

    private EventReplayStep(final MessageStore.Poller eventPoller, final EventHandler eventHandler) {
        this.eventPoller = requireNonNull(eventPoller);
        this.eventHandler = requireNonNull(eventHandler);
    }

    public static EventReplayStep replayNonAbortedEvents(final MessageStore eventStore, final EventHandler eventHandler) {
        return new EventReplayStep(new CommittedEventPoller(eventStore), eventHandler);
    }

    public static EventReplayStep replayAllEvents(final MessageStore eventStore, final EventHandler eventHandler) {
        return replayAllEvents(eventStore.poller(), eventHandler);
    }

    public static EventReplayStep replayAllEvents(final MessageStore.Poller eventPoller, final EventHandler eventHandler) {
        return new EventReplayStep(eventPoller, eventHandler);
    }

    @Override
    public int doWork() {
        if (replayed) {
            return 0;
        }
        final int polled = eventPoller.poll(pollerHandler);
        if (polled == 0) {
            eventPoller.close();
            replayed = true;
        }
        return polled;
    }

    private Result onEvent(final DirectBuffer event) {
        eventHandler.onEvent(flyweightEvent.wrap(event, 0));
        return Result.POLL;
    }

}
