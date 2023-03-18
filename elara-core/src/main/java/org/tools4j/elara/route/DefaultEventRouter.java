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
package org.tools4j.elara.route;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.flyweight.FlyweightHeader;
import org.tools4j.elara.plugin.base.BaseEvents;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.store.ExpandableDirectBuffer;
import org.tools4j.elara.store.MessageStore.Appender;
import org.tools4j.elara.store.MessageStore.AppendingContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.EventDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.EventDescriptor.PAYLOAD_OFFSET;

public class DefaultEventRouter implements EventRouter.Default {

    private static final int MAX_EVENTS = Short.MAX_VALUE;

    private final TimeSource timeSource;
    private final BaseState baseState;
    private final Appender appender;
    private final EventApplier eventApplier;
    private final RoutingContext routingContext = new RoutingContext();

    private Command command;
    private int nextIndex;
    private boolean skipped;

    public DefaultEventRouter(final TimeSource timeSource,
                              final BaseState baseState,
                              final Appender appender,
                              final EventApplier eventApplier) {
        this.timeSource = requireNonNull(timeSource);
        this.baseState = requireNonNull(baseState);
        this.appender = requireNonNull(appender);
        this.eventApplier = requireNonNull(eventApplier);
    }

    public DefaultEventRouter start(final Command command) {
        this.command = requireNonNull(command);
        this.nextIndex = 0;
        this.skipped = false;
        return this;
    }

    @Override
    public RoutingContext routingEvent(final int type) {
        checkValidCommand();
        checkNotSkipped();
        checkEventLimit();
        return routingEvent0(type);
    }

    private RoutingContext routingEvent0(final int payloadType) {
        if (nextIndex > 0 && routingContext.isCommitPending()) {
            routingContext.commit(false);
        }
        return routingContext.init(payloadType, appender.appending());
    }

    public void complete() {
        if (!skipped) {
            //we need a commit event if (a) there was no event, or (b) if routing of the last event was aborted
            if (nextIndex == 0 || !routingContext.isCommitPending()) {
                routeCommitEvent();
            }
            routingContext.commit(true);
        }
        this.command = null;
        this.skipped = false;
        this.nextIndex = 0;
    }

    private void routeCommitEvent() {
        try (final EventRouter.RoutingContext context = routingEvent0(BaseEvents.AUTO_COMMIT)) {
            context.route(0);
        }
    }

    @Override
    public boolean skipCommand() {
        if (skipped) {
            return true;
        }
        if (nextIndex > 0) {
            return false;
        }
        if (routingContext.isCommitPending()) {
            routingContext.abort();
        }
        skipped = true;
        return skipped;
    }

    @Override
    public boolean isSkipped() {
        return skipped;
    }

    @Override
    public long nextEventSequence() {
        return baseState.lastAppliedEventSequence() + 1;
    }

    @Override
    public int nextEventIndex() {
        return nextIndex;
    }

    @Override
    public Command command() {
        checkValidCommand();
        return command;
    }

    private void checkValidCommand() {
        if (command == null) {
            throw new IllegalStateException("No command is currently associated with this event router");
        }
    }
    private void checkNotSkipped() {
        if (skipped) {
            throw new IllegalStateException("Command has been skipped and event routing is not possible");
        }
    }

    private void checkEventLimit() {
        if (nextIndex >= MAX_EVENTS) {
            throw new IllegalStateException("Maximum number of events per command reached: " + MAX_EVENTS);
        }
    }

    private final class RoutingContext implements EventRouter.RoutingContext {

        final FlyweightEvent flyweightEvent = new FlyweightEvent();
        final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        AppendingContext context;

        RoutingContext init(final int payloadType, final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Routing context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightEvent.writeHeader(
                    command.sourceId(), command.sourceSequence(), nextIndex, false, nextEventSequence(),
                    timeSource.currentTime(), payloadType, 0, context.buffer(), HEADER_OFFSET
            );
            return this;
        }

        void commit(final boolean last) {
            try (final AppendingContext ac = unclosedContext()) {
                if (last) {
                    assert nextIndex > 0;
                    FlyweightEvent.writeIndex((short)(nextIndex - 1), true, ac.buffer());
                }
                ac.commit(FlyweightHeader.frameSize(ac.buffer()));
            } finally {
                context = null;
            }
        }

        AppendingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Routing context is closed");
        }

        @Override
        public int index() {
            unclosedContext();
            return nextIndex;
        }

        @Override
        public MutableDirectBuffer buffer() {
            unclosedContext();
            return buffer;
        }

        @Override
        public void route(final int length) {
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            final AppendingContext ac = unclosedContext();
            buffer.unwrap();
            if (length > 0) {
                FlyweightEvent.writePayloadSize(length, ac.buffer());
            }
            flyweightEvent.wrapSilently(ac.buffer(), HEADER_OFFSET);
            eventApplier.onEvent(flyweightEvent);
            flyweightEvent.reset();
            ++nextIndex;
        }

        @Override
        public void abort() {
            if (context != null) {
                buffer.unwrap();
                flyweightEvent.reset();
                try {
                    context.abort();
                } finally {
                    context = null;
                }
            }
        }

        @Override
        public boolean isClosed() {
            return context == null || buffer.buffer() == null;
        }

        boolean isCommitPending() {
            return buffer.buffer() == null && context != null;
        }
    }
}
