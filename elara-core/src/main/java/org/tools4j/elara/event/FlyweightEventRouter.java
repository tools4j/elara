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
package org.tools4j.elara.event;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.log.MessageLog;

import static java.util.Objects.requireNonNull;

public class FlyweightEventRouter implements EventRouter {

    private final MessageLog.Appender<? super Event> eventLogAppender;
    private final MutableDirectBuffer headerBuffer = new ExpandableDirectByteBuffer(FlyweightEvent.HEADER_LENGTH);
    private final FlyweightEvent flyweightEvent = new FlyweightEvent();

    private Command command;
    private int index = 0;

    public FlyweightEventRouter(final MessageLog.Appender<? super Event> eventLogAppender) {
        this.eventLogAppender = requireNonNull(eventLogAppender);
    }

    public FlyweightEventRouter start(final Command command) {
        this.command = requireNonNull(command);
        this.index = 0;
        return this;
    }

    @Override
    public void routeEvent(final int type, final DirectBuffer event, final int offset, final int length) {
        checkAllowedType(type);
        eventLogAppender.append(flyweightEvent.init(
                headerBuffer, 0, command.id().input(), command.id().sequence(), index, type,
                command.time(), event, offset, length
        ));
        this.flyweightEvent.reset();
        index++;
    }

    public FlyweightEventRouter commit() {
        eventLogAppender.append(AdminEvents.noop(flyweightEvent, headerBuffer, 0, command, index));
        this.flyweightEvent.reset();
        this.command = null;
        this.index = 0;
        return this;
    }

    private void checkAllowedType(final int eventType) {
        if (eventType == EventType.NOOP.value()) {
            throw new IllegalArgumentException("Illegal event type: " + eventType);
        }
    }
}
