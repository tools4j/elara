/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.log.MessageLog.Appender;
import org.tools4j.elara.log.MessageLog.AppendingContext;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;

public class DefaultEventRouter implements EventRouter.Default {

    private static final int MAX_EVENTS = Short.MAX_VALUE + 1;

    private final Appender appender;
    private final EventApplier eventApplier;
    private final RoutingContext routingContext = new RoutingContext();

    private Command command;
    private short nextIndex;
    private boolean skipped;

    public DefaultEventRouter(final Appender appender, final EventApplier eventApplier) {
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
        checkEventType(type);
        checkNotSkipped();
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
        if (!skipped) {
            //we need a commit event if (a) there was no event, or (b) if routing of the last event was aborted
            if (nextIndex == 0 || !routingContext.isCommitPending()) {
                routeCommitEvent();
            }
            routingContext.commit(Flags.COMMIT);
        }
        this.command = null;
        this.skipped = false;
        this.nextIndex = 0;
    }

    private void routeCommitEvent() {
        try (final EventRouter.RoutingContext context = routingEvent0(EventType.COMMIT)) {
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
    public short nextEventIndex() {
        return nextIndex;
    }

    private static void checkEventType(final int eventType) {
        if (eventType == EventType.COMMIT || eventType == EventType.ROLLBACK) {
            throw new IllegalArgumentException("Illegal event type: " + eventType);
        }
    }

    private void checkNotSkipped() {
        if (skipped) {
            throw new IllegalStateException("Command has been skipped and event routing is not possible");
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
        AppendingContext context;

        RoutingContext init(final int type, final AppendingContext context) {
            if (this.context != null) {
                abort();
                throw new IllegalStateException("Routing context not closed");
            }
            this.context = requireNonNull(context);
            this.buffer.wrap(context.buffer(), PAYLOAD_OFFSET);
            FlyweightHeader.writeTo(
                    command.id().source(), type, command.id().sequence(), command.time(),
                    Flags.NONE, nextIndex, 0,
                    context.buffer(), HEADER_OFFSET
            );
            return this;
        }

        void commit(final byte flags) {
            try (final AppendingContext ac = unclosedContext()) {
                if (flags != Flags.NONE) {
                    ac.buffer().putByte(FLAGS_OFFSET, flags);
                }
                final int payloadLength = ac.buffer().getInt(PAYLOAD_SIZE_OFFSET);
                ac.commit(HEADER_LENGTH + payloadLength);
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
                ac.buffer().putInt(PAYLOAD_SIZE_OFFSET, length);
            }
            flyweightEvent.init(ac.buffer(), 0);
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
