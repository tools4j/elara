/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.log;

import org.agrona.DirectBuffer;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.log.MessageLog.Poller;

import static org.tools4j.elara.log.MessageLog.Handler.Result.POLL;

/**
 * An event poller that works with two underlying pollers to ensure only committed events
 * are passed to the event handler.  The lookahead poller reads ahead until either a commit or
 * abort event is found; the event poller then invokes the handler for the corresponding events
 * or skips them if they were aborted.
 */
public class CommittedEventPoller implements Poller {

    private final Poller aheadPoller;
    private final Poller eventPoller;

    private final LookAheadState aheadState = new LookAheadState();

    public CommittedEventPoller(final MessageLog eventLog) {
        this.aheadPoller = eventLog.poller();
        this.eventPoller = eventLog.poller();
    }

    public CommittedEventPoller(final MessageLog eventLog, final String id) {
        this.eventPoller = eventLog.poller(id);
        this.aheadPoller = eventLog.poller();
        aheadPoller.moveTo(eventPoller.entryId());
    }

    @Override
    public long entryId() {
        return eventPoller.entryId();
    }

    @Override
    public Poller moveToStart() {
        aheadPoller.moveToStart();
        eventPoller.moveToStart();
        aheadState.reset();
        return this;
    }

    @Override
    public Poller moveToEnd() {
        while (poll(event -> POLL) > 0);
        return this;
    }

    @Override
    public boolean moveToNext() {
        final long entryId = entryId();
        while (poll(event -> POLL) > 0) {
            if (entryId() != entryId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean moveToPrevious() {
        return eventPoller.moveToPrevious();
    }

    @Override
    public boolean moveTo(final long entryId) {
        final long curHeadId = aheadPoller.entryId();
        if (!aheadPoller.moveTo(entryId)) {
            aheadPoller.moveTo(curHeadId);
            return false;
        }
        byte lastEventFlags = aheadState.lastEventFlags;
        aheadState.reset();
        while (aheadPoller.poll(aheadState) > 0) {
            if (aheadState.isCommit()) {
                eventPoller.moveTo(entryId);
                return true;
            }
            if (aheadState.isRollback()) {
                //event with this entryId exists but it was rolled back
                break;
            }
        }
        aheadState.lastEventFlags = lastEventFlags;
        aheadPoller.moveTo(curHeadId);
        return false;
    }

    @Override
    public int poll(final MessageLog.Handler handler) {
        if (eventPoller.entryId() == aheadPoller.entryId()) {
            aheadState.reset();
        }
        if (aheadState.isCommit()) {
            return eventPoller.poll(handler);
        }
        if (aheadState.isRollback()) {
            eventPoller.moveTo(aheadPoller.entryId());
            aheadState.reset();
        }
        return aheadPoller.poll(aheadState);
    }

    private static class LookAheadState implements MessageLog.Handler {
        byte lastEventFlags;
        final FlyweightEvent event = new FlyweightEvent();

        @Override
        public Result onMessage(final DirectBuffer buffer) {
            onEvent(event.init(buffer, 0));
            return POLL;
        }

        void onEvent(final Event event) {
            lastEventFlags = event.flags().value();
        }
        void reset() {
            lastEventFlags = Flags.NONE;
        }
        boolean isCommit() {
            return Flags.isCommit(lastEventFlags);
        }
        boolean isRollback() {
            return Flags.isRollback(lastEventFlags);
        }
    }
}
