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
package org.tools4j.elara.store;

import org.agrona.DirectBuffer;
import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.store.MessageStore.Poller;
import org.tools4j.elara.stream.MessageReceiver;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;
import static org.tools4j.elara.sequence.SequenceSupplier.NIL_SEQUENCE;
import static org.tools4j.elara.store.MessageStore.Handler.Result.POLL;

/**
 * An event poller that works with two underlying pollers, one to play back events and a look-ahead poller to track
 * latest source and event sequence numbers.
 */
public class PlaybackEventPoller implements MessageStore.Poller, MessageReceiver {

    public static final int DEFAULT_INITIAL_SOURCE_CAPACITY = 32;

    private final Poller aheadPoller;
    private final Poller eventPoller;
    private final LookAheadState aheadState;
    private final ReceiverHandlerAdapter receiverHandlerAdapter = new ReceiverHandlerAdapter();

    public PlaybackEventPoller(final MessageStore eventStore) {
        this(eventStore, DEFAULT_INITIAL_SOURCE_CAPACITY);
    }

    public PlaybackEventPoller(final MessageStore eventStore, final int initialSourceCapacity) {
        this.aheadPoller = eventStore.poller();
        this.eventPoller = eventStore.poller();
        this.aheadState = new LookAheadState(initialSourceCapacity);
    }

    public Poller reset() {
        aheadPoller.moveToStart();
        eventPoller.moveToStart();
        aheadState.reset();
        return this;
    }

    @Override
    public long entryId() {
        return eventPoller.entryId();
    }

    @Override
    public Poller moveToStart() {
        moveAhead();
        eventPoller.moveToStart();
        return this;
    }

    @Override
    public Poller moveToEnd() {
        moveAhead();
        eventPoller.moveToEnd();
        return this;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    private void moveAhead() {
        while (aheadPoller.poll(aheadState) > 0);
    }

    @Override
    public boolean moveToNext() {
        moveAhead();
        return eventPoller.moveToNext();
    }

    @Override
    public boolean moveToPrevious() {
        moveAhead();
        return eventPoller.moveToPrevious();
    }

    @Override
    public boolean moveTo(final long entryId) {
        moveAhead();
        return eventPoller.moveTo(entryId);
    }

    @Override
    public int poll(final MessageStore.Handler handler) {
        moveAhead();
        return eventPoller.poll(handler);
    }

    @Override
    public int poll(final MessageReceiver.Handler handler) {
        return poll(receiverHandlerAdapter.init(handler));
    }

    @Override
    public boolean isClosed() {
        return aheadPoller.isClosed();
    }

    @Override
    public void close() {
        aheadPoller.close();
        eventPoller.close();
    }

    public long maxAvailableEventSequence() {
        return aheadState.maxAvailableEventSeq;
    }

    public long maxAvailableSourceSequence(final int sourceId) {
        return aheadState.maxAvailableSourceSeq.get(sourceId);
    }

    public long newestEventTime() {
        return aheadState.newestEventTime;
    }

    private static class ReceiverHandlerAdapter {
        MessageReceiver.Handler receiverHandler;
        final MessageStore.Handler pollerHandler = this::onMessage;

        MessageStore.Handler init(final MessageReceiver.Handler receiverHandler) {
            requireNonNull(receiverHandler);
            if (this.receiverHandler != receiverHandler) {
                this.receiverHandler = receiverHandler;
            }
            return pollerHandler;
        }

        Result onMessage(final DirectBuffer buffer) {
            receiverHandler.onMessage(buffer);
            return POLL;
        }
    }

    private static class LookAheadState implements MessageStore.Handler {
        long newestEventTime;
        long maxAvailableEventSeq;
        final Long2LongHashMap maxAvailableSourceSeq;
        final FlyweightEvent event = new FlyweightEvent();

        LookAheadState(final int initialSourceCapacity) {
            this.maxAvailableSourceSeq = new Long2LongHashMap(initialSourceCapacity, DEFAULT_LOAD_FACTOR, NIL_SEQUENCE);
            reset();
        }

        void reset() {
            newestEventTime = TimeSource.MIN_VALUE;
            maxAvailableEventSeq = NIL_SEQUENCE;
            maxAvailableSourceSeq.clear();
            event.reset();
        }

        @Override
        public Result onMessage(final DirectBuffer buffer) {
            onEvent(event.wrapSilently(buffer, 0));
            return POLL;
        }

        void onEvent(final Event event) {
            newestEventTime = event.eventTime();
            maxAvailableEventSeq = event.eventSequence();
            maxAvailableSourceSeq.put(event.sourceId(), event.sourceSequence());
        }
    }
}
