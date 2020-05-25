/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.flyweight.FlyweightHeader;
import org.tools4j.elara.log.ExpandableDirectBuffer;
import org.tools4j.elara.log.MessageLog.AppendContext;
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.plugin.base.BaseState;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.route.SkipMode.CONFLATED;
import static org.tools4j.elara.route.SkipMode.CONSUMED;
import static org.tools4j.elara.route.SkipMode.NONE;
import static org.tools4j.elara.route.SkipMode.SKIPPED;

public class DefaultEventRouter implements EventRouter.Default {

    private static final int MAX_EVENTS = Short.MAX_VALUE + 1;

    private final BaseState baseState;
    private final Appender appender;
    private final EventApplier eventApplier;
    private final RoutingContext routingContext = new RoutingContext();

    private Command command;
    private SkipMode skipMode = NONE;
    private short nextIndex = 0;

    public DefaultEventRouter(final BaseState baseState, final Appender appender, final EventApplier eventApplier) {
        this.baseState = requireNonNull(baseState);
        this.appender = requireNonNull(appender);
        this.eventApplier = requireNonNull(eventApplier);
    }

    public DefaultEventRouter start(final Command command) {
        this.command = requireNonNull(command);
        this.nextIndex = 0;
        this.skipMode = NONE;
        return this;
    }

    @Override
    public RoutingContext routingEvent(final int type) {
        checkEventType(type);
        checkEventLimit();
        return routingEvent0(type);
    }

    private RoutingContext routingEvent0(final int type) {
        if (nextIndex > 0 && routingContext.isCommitPending()) {
            routingContext.commit(Flags.NONE);
        }
        return routingContext.init(type, appender.appending());
    }

    public void complete() {
        if (skipMode == NONE) {
            if (nextIndex == 0 || !routingContext.isCommitPending()) {
                routeCommitEvent();
            }
            routingContext.commit(Flags.COMMIT);
        }
        this.command = null;
        this.skipMode = NONE;
        this.nextIndex = 0;
    }

    private void routeCommitEvent() {
        try (final EventRouter.RoutingContext context = routingEvent0(EventType.COMMIT)) {
            context.route(0);
        }
    }

    @Override
    public SkipMode skipCommand(final boolean allowConflation) {
        return skip(allowConflation, false);
    }

    @Override
    public SkipMode skipFurtherCommandEvents() {
        return skip(false, true);
    }

    private SkipMode skip(final boolean allowConflation, final boolean commitPendingEvents) {
        if (skipMode != NONE) {
            return skipMode;
        }
        if (routingContext.isCommitPending()) {
            if (commitPendingEvents) {
                routingContext.commit(Flags.COMMIT);
            } else {
                routingContext.abort();
            }
        }
        if (nextIndex == 0 && !allowConflation) {
            routeCommitEvent();
            routingContext.commit(Flags.COMMIT);
            skipMode = SKIPPED;
        } else {
            skipMode = nextIndex == 0 ? CONFLATED : CONSUMED;
        }
        return skipMode;
    }

    @Override
    public SkipMode skipMode() {
        return skipMode;
    }

    @Override
    public short nextEventIndex() {
        return nextIndex;
    }

    @Override
    public long lastAppliedCommandSequenceForSameInput() {
        return baseState.lastProcessedCommandSequenceForInput(command.id().input());
    }

    private static void checkEventType(final int eventType) {
        if (eventType == EventType.COMMIT) {
            throw new IllegalArgumentException("Illegal event type: " + eventType);
        }
    }

    private void checkEventLimit() {
        if ((0xffff & nextIndex) >= MAX_EVENTS) {
            throw new IllegalStateException("Maximum number of events per command reached: " + MAX_EVENTS);
        }
    }

    private final class RoutingContext implements EventRouter.RoutingContext {

        final FlyweightEvent flyweightEvent = new FlyweightEvent();
        final ExpandableDirectBuffer buffer = new ExpandableDirectBuffer();
        AppendContext context;

        RoutingContext init(final int type, final AppendContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Routing context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightHeader.writeTo(
                    command.id().input(), type, command.id().sequence(), command.time(),
                    Flags.NONE, nextIndex, 0,
                    context.buffer(), HEADER_OFFSET
            );
            return this;
        }

        void commit(final byte flags) {
            ensureNotClosed();
            if (flags != Flags.NONE) {
                context.buffer().putByte(FLAGS_OFFSET, flags);
            }
            final int payloadLength = context.buffer().getInt(PAYLOAD_SIZE_OFFSET);
            context.commit(HEADER_LENGTH + payloadLength);
            context = null;
        }

        void ensureNotClosed() {
            if (context != null) {
                return;
            }
            throw new IllegalStateException("Routing context is closed");
        }

        @Override
        public int index() {
            ensureNotClosed();
            return nextIndex;
        }

        @Override
        public MutableDirectBuffer buffer() {
            ensureNotClosed();
            return buffer;
        }

        @Override
        public void route(final int length) {
            if (skipMode != NONE) {
                abort();
                return;
            }
            ensureNotClosed();
            buffer.unwrap();
            context.buffer().putInt(PAYLOAD_SIZE_OFFSET, length);
            flyweightEvent.init(context.buffer(), 0);
            eventApplier.onEvent(flyweightEvent);
            flyweightEvent.reset();
            ++nextIndex;
        }

        @Override
        public void abort() {
            if (context != null) {
                buffer.unwrap();
                flyweightEvent.reset();
                context.abort();
                context = null;
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
