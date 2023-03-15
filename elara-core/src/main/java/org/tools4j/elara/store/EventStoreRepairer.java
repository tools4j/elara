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
package org.tools4j.elara.store;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.Event.Flags;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.plugin.base.BaseEvents;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.store.MessageStore.Poller;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;

/**
 * Class to repair an event store that was corrupted usually due to application crash.  A corrupted event store is an
 * event store that is non-empty and whose last event entry has neither the commit nor the rollback flag set.
 *
 * @see Flags#isLast()
 */
public class EventStoreRepairer {

    private static final int MAX_EVENT_INDEX = Short.MAX_VALUE;

    private final MessageStore eventStore;
    private final FlyweightEvent lastNonFinalEventOrNull;

    /**
     * Initialises this repairer with the given {@code eventStore}
     *
     * @param eventStore the event store to inspect and prepare for reparation
     * @throws IllegalArgumentException if {@code eventStore} is not a valid event store or a wroong version or if it cannot
     *                                  be repaired
     */
    public EventStoreRepairer(final MessageStore eventStore) {
        this.eventStore = requireNonNull(eventStore);
        this.lastNonFinalEventOrNull = lastNonFinalEventOrNull(eventStore);
    }

    public boolean isCorrupted() {
        return null != lastNonFinalEventOrNull;
    }

    /**
     * Repairs the event store if necessary and leaves it an uncorrupted state.
     *
     * @return true if any modification was necessary, and false if the event store was uncorrupted
     */
    public boolean repair() {
        if (lastNonFinalEventOrNull != null) {
            final FlyweightEvent event = lastNonFinalEventOrNull;
            final int nextIndex = lastNonFinalEventOrNull.index() + 1;
            if (nextIndex > MAX_EVENT_INDEX) {
                //should not get here since we checked on init
                throw new RuntimeException("Event index " + nextIndex + " exceeds max allowed " + MAX_EVENT_INDEX);
            }
            try (final AppendingContext context = eventStore.appender().appending()) {
                BaseEvents.rollback(event, context.buffer(), 0, event.sourceId(), event.sourceSequence(),
                        (short)nextIndex, event.eventTime());
                context.commit(HEADER_LENGTH);
            }
            return true;
        }
        return false;
    }

    private static FlyweightEvent lastNonFinalEventOrNull(final MessageStore eventStore) {
        final Poller poller = eventStore.poller();
        poller.moveToEnd();
        if (poller.moveToPrevious()) {
            final FlyweightEvent event = new FlyweightEvent();
            if (0 == poller.poll(message -> {
                final int length = message.capacity();
                final MutableDirectBuffer copy = new ExpandableArrayBuffer(length);
                message.getBytes(0, copy, 0, length);
                event.wrap(copy, 0);
                return Result.POLL;
            })) {
                throw new RuntimeException("Poller should have returned last event");
            }
            if (event.index() < 0) {
                throw new IllegalArgumentException("Not an event store (is it a command store?): " + eventStore);
            }
            if (event.flags().isLast()) {
                return null;
            }
            if (event.index() >= MAX_EVENT_INDEX) {
                throw new IllegalArgumentException("Event store cannot be repaired, last event has maximum allowed index: " +
                        event.index());
            }
            return event;
        }
        return null;
    }
}
