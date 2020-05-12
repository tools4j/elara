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

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameDescriptor.FLAGS_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.flyweight.FrameDescriptor.HEADER_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_OFFSET;
import static org.tools4j.elara.flyweight.FrameDescriptor.PAYLOAD_SIZE_OFFSET;
import static org.tools4j.elara.route.RollbackMode.REPLAY_COMMAND;
import static org.tools4j.elara.route.StateImpact.STATE_CORRUPTION_POSSIBLE;
import static org.tools4j.elara.route.StateImpact.STATE_UNAFFECTED;

public class FlyweightEventRouter implements EventRouter {

    private final Appender appender;
    private final EventApplier eventApplier;
    private final RoutingContext routingContext = new RoutingContext();

    private Command command;
    private RollbackMode rollbackMode;
    private short nextIndex = 0;

    public FlyweightEventRouter(final Appender appender, final EventApplier eventApplier) {
        this.appender = requireNonNull(appender);
        this.eventApplier = requireNonNull(eventApplier);
    }

    public FlyweightEventRouter start(final Command command) {
        this.command = requireNonNull(command);
        this.nextIndex = 0;
        this.rollbackMode = null;
        return this;
    }

    @Override
    public RoutingContext routingEvent(final int type) {
        checkEventType(type);
        checkNotRolledBack();
        return routingEvent0(type);
    }

    private RoutingContext routingEvent0(final int type) {
        if (nextIndex > 0 && routingContext.isCommitPending()) {
            routingContext.commit(Flags.NONE);
        }
        return routingContext.init(type, appender.appending());
    }

    public boolean complete() {
        final RollbackMode mode = rollbackMode;
        if (mode == null) {
            if (nextIndex == 0 || !routingContext.isCommitPending()) {
                try (final EventRouter.RoutingContext context = routingEvent0(EventType.COMMIT)) {
                    context.route(0);
                }
            }
            routingContext.commit(Flags.COMMIT);
        } else {
            if (routingContext.isCommitPending()) {
                routingContext.abort();
            }
            if (nextIndex > 0 || mode != REPLAY_COMMAND) {
                try (final EventRouter.RoutingContext context = routingEvent0(EventType.ROLLBACK)) {
                    context.route(0);
                }
                routingContext.commit(Flags.ROLLBACK);
            }
        }
        this.command = null;
        this.rollbackMode = null;
        this.nextIndex = 0;
        return mode != REPLAY_COMMAND;
    }

    @Override
    public StateImpact rollbackAfterProcessing(final RollbackMode mode) {
        this.rollbackMode = requireNonNull(mode);
        return nextIndex == 0 ? STATE_UNAFFECTED : STATE_CORRUPTION_POSSIBLE;
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

    private void checkNotRolledBack() {
        if (rollbackMode != null) {
            throw new IllegalStateException("It is illegal to route events after setting rollback mode");
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
        public MutableDirectBuffer payload() {
            ensureNotClosed();
            return buffer;
        }

        @Override
        public void route(final int payloadLength) {
            ensureNotClosed();
            buffer.unwrap();
            context.buffer().putInt(PAYLOAD_SIZE_OFFSET, payloadLength);
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
