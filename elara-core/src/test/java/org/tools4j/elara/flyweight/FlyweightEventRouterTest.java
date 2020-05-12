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
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.log.InMemoryLog;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.route.FlyweightEventRouter;
import org.tools4j.elara.route.RollbackMode;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test for {@link FlyweightEventRouter}
 */
public class FlyweightEventRouterTest {

    private MessageLog messageLog;
    private List<Event> routed;
    private FlyweightCommand command;

    //under test
    private FlyweightEventRouter eventRouter;

    @BeforeEach
    public void init() {
        routed = new ArrayList<>();
        command = new FlyweightCommand();
        messageLog = new InMemoryLog();
        eventRouter = new FlyweightEventRouter(messageLog.appender(), event -> {
            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
            event.writeTo(buffer, 0);
            routed.add(new FlyweightEvent().init(buffer, 0));
        });
    }

    @Test
    public void noEvents() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;

        //when
        startWithCommand(input, sequence, type, time);

        //then
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[0]");

        //when
        final boolean result = eventRouter.complete();

        //then
        assertTrue(result, "result");
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[x]");
        assertEquals(1, routed.size(), "events.size");
        assertEvent(command, routed.get(0), EventType.COMMIT, 0, Flags.COMMIT, 0, "events[0]");
    }

    @Test
    public void someEvents() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;
        final int eventType = 55;
        final int msgOffset = 2;
        final String msg = "Hello world";

        startWithCommand(input, sequence, type, time);

        //when
        routeEmptyApplicationEvent();

        //then
        assertEquals(1, eventRouter.nextEventIndex(), "commitIndex[1]");

        //when
        final int payloadSize = routeEvent(eventType, msgOffset,  msg);

        //then
        assertEquals(2, eventRouter.nextEventIndex(), "commitIndex[2]");

        //when
        final boolean result = eventRouter.complete();

        //then
        assertTrue(result, "result");
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[x]");
        assertEquals(2, routed.size(), "events.size");
        assertEvent(command, routed.get(0), EventType.APPLICATION, 0, Flags.NONE, 0, "events[0]");
        assertEvent(command, routed.get(1), eventType, 1, Flags.COMMIT, payloadSize, "events[1]");
        assertEquals(msg, routed.get(1).payload().getStringAscii(msgOffset), "events[1].payload.msg");
    }

    @Test
    public void rollbackNoEventsSkip() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;

        startWithCommand(input, sequence, type, time);

        //when
        eventRouter.rollbackAfterProcessing(RollbackMode.SKIP_COMMAND);
        final boolean result = eventRouter.complete();

        //then
        assertTrue(result, "result");
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[x]");
        assertEquals(1, routed.size(), "events.size");
        assertEvent(command, routed.get(0), EventType.ROLLBACK, 0, Flags.ROLLBACK, 0, "events[0]");
    }

    @Test
    public void rollbackSomeEventsReplay() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;
        final int eventType = 55;
        final int msgOffset = 2;
        final String msg = "Hello world";

        startWithCommand(input, sequence, type, time);
        routeEmptyApplicationEvent();
        final int payloadSize = routeEvent(eventType, msgOffset, msg);

        //when
        eventRouter.rollbackAfterProcessing(RollbackMode.REPLAY_COMMAND);
        final boolean result = eventRouter.complete();

        //then
        assertFalse(result, "result");
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[x]");
        assertEquals(3, routed.size(), "events.size");
        assertEvent(command, routed.get(0), EventType.APPLICATION, 0, Flags.NONE, 0, "events[0]");
        assertEvent(command, routed.get(1), eventType, 1, Flags.NONE, payloadSize, "events[1]");
        assertEvent(command, routed.get(2), EventType.ROLLBACK, 2, Flags.ROLLBACK, 0, "events[2]");

    }

    private void startWithCommand(final int input,
                                  final long sequence,
                                  final int type,
                                  final long time) {
        command.init(new ExpandableArrayBuffer(), 0, input, sequence, type, time,
                new ExpandableArrayBuffer(12), 2, 10);
        eventRouter.start(command);
    }

    private void routeEmptyApplicationEvent() {
        final DirectBuffer zeroPayload = new ExpandableArrayBuffer(0);
        eventRouter.routeEvent(zeroPayload, 0, 0);
    }

    private int routeEvent(final int eventType, final int msgOffset, final String msg) {
        final MutableDirectBuffer msgPayload = new ExpandableArrayBuffer();
        final int msgLength = msgPayload.putStringAscii(msgOffset, msg);
        final int payloadSize = msgOffset + msgLength;
        eventRouter.routeEvent(eventType, msgPayload, 0, payloadSize);
        return payloadSize;
    }

    @Test
    public void commitEventNotAllowed() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;
        final DirectBuffer zeroPayload = new ExpandableArrayBuffer(0);
        startWithCommand(input, sequence, type, time);

        //when
        assertThrows(IllegalArgumentException.class,
                () -> eventRouter.routeEvent(EventType.COMMIT, zeroPayload, 0, 0)
        );
    }

    @Test
    public void rollbackEventNotAllowed() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;
        final DirectBuffer zeroPayload = new ExpandableArrayBuffer(0);
        startWithCommand(input, sequence, type, time);

        //when
        assertThrows(IllegalArgumentException.class,
                () -> eventRouter.routeEvent(EventType.ROLLBACK, zeroPayload, 0, 0)
        );
    }

    private void assertEvent(final Command command, final Event event,
                             final int type, final int index, final byte flags, final int payloadSize,
                             final String evtName) {
        assertEquals(command.id().input(), event.id().commandId().input(), evtName + ".id.commandId.input");
        assertEquals(command.id().sequence(), event.id().commandId().sequence(), evtName + ".id.commandId.sequence");
        assertEquals(index, event.id().index(), evtName + ".id.index");
        assertEquals(type, event.type(), evtName + ".type");
        assertEquals(Flags.NONE, event.flags().value(), evtName + ".flags");
        assertEquals(command.time(), event.time(), evtName + ".time");
        assertEquals(payloadSize, event.payload().capacity(), evtName + ".payload.capacity");
    }
}