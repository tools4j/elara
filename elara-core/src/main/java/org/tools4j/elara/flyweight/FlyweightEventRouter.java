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
package org.tools4j.elara.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.EventHandler;
import org.tools4j.elara.event.EventRouter;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.event.RollbackMode;
import org.tools4j.elara.event.StateImpact;
import org.tools4j.elara.plugin.base.BaseEvents;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.event.RollbackMode.REPLAY_COMMAND;
import static org.tools4j.elara.event.StateImpact.STATE_CORRUPTION_POSSIBLE;
import static org.tools4j.elara.event.StateImpact.STATE_UNAFFECTED;

public class FlyweightEventRouter implements EventRouter {

    private final EventHandler eventHandler;
    private final MutableDirectBuffer headerBuffer = new ExpandableDirectByteBuffer(FrameDescriptor.HEADER_LENGTH);
    private final FlyweightEvent flyweightEvent = new FlyweightEvent();

    private Command command;
    private RollbackMode rollbackMode;
    private short nextIndex = 0;

    public FlyweightEventRouter(final EventHandler eventHandler) {
        this.eventHandler = requireNonNull(eventHandler);
    }

    public FlyweightEventRouter start(final Command command) {
        this.command = requireNonNull(command);
        this.nextIndex = 0;
        this.rollbackMode = null;
        return this;
    }

    @Override
    public void routeEvent(final int type, final DirectBuffer event, final int offset, final int length) {
        checkEventType(type);
        checkNotRolledBack();
        eventHandler.onEvent(flyweightEvent.init(
                headerBuffer, 0, command.id().input(), command.id().sequence(), nextIndex, type,
                command.time(), Flags.NONE, event, offset, length
        ));
        this.flyweightEvent.reset();
        nextIndex++;
    }

    @Override
    public RouteContext routingEvent(final int type) {
        throw new UnsupportedOperationException();//FIXME implement
    }

    public boolean complete() {
        final RollbackMode mode = rollbackMode;
        if (mode == null) {
            eventHandler.onEvent(BaseEvents.commit(flyweightEvent, headerBuffer, 0, command, nextIndex));
        } else {
            if (nextIndex > 0 || mode != REPLAY_COMMAND) {
                eventHandler.onEvent(BaseEvents.rollback(flyweightEvent, headerBuffer, 0, command, nextIndex));
            }
        }
        this.flyweightEvent.reset();
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
}
