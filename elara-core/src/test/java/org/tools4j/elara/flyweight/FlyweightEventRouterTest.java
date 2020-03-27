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

import java.util.ArrayDeque;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit test for {@link FlyweightEventRouter}
 */
public class FlyweightEventRouterTest {

    private Queue<Event> events;
    private FlyweightCommand command;

    //under test
    private FlyweightEventRouter eventRouter;

    @BeforeEach
    public void init() {
        events = new ArrayDeque<>();
        command = new FlyweightCommand();
        eventRouter = new FlyweightEventRouter(event -> {
            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
            event.writeTo(buffer, 0);
            events.add(new FlyweightEvent().init(buffer, 0));
        });
    }

    @Test
    public void noEvents() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;
        command.init(new ExpandableArrayBuffer(), 0, input, sequence, type, time,
                new ExpandableArrayBuffer(12), 2, 10);

        //when
        eventRouter.start(command);

        //then
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[0]");

        //when
        eventRouter.commit();

        //then
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[x]");
        assertEquals(1, events.size(), "events.size");
        assertEvent(command, events.poll(), EventType.COMMIT, 0, 0, "events[0]");
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
        command.init(new ExpandableArrayBuffer(), 0, input, sequence, type, time,
                new ExpandableArrayBuffer(12), 2, 10);
        final DirectBuffer zeroPayload = new ExpandableArrayBuffer(0);
        final MutableDirectBuffer msgPayload = new ExpandableArrayBuffer(20);
        msgPayload.putStringAscii(msgOffset, msg);

        //when
        eventRouter.start(command);
        eventRouter.routeEvent(zeroPayload, 0, 0);

        //then
        assertEquals(1, eventRouter.nextEventIndex(), "commitIndex[1]");

        //when
        eventRouter.routeEvent(eventType, msgPayload, 0, 20);

        //then
        assertEquals(2, eventRouter.nextEventIndex(), "commitIndex[2]");

        //when
        eventRouter.commit();

        //then
        assertEquals(0, eventRouter.nextEventIndex(), "commitIndex[x]");
        assertEquals(3, events.size(), "events.size");
        assertEvent(command, events.poll(), EventType.APPLICATION, 0, 0, "events[0]");
        assertEvent(command, events.peek(), eventType, 1, 20, "events[1]");
        assertEquals(msg, events.poll().payload().getStringAscii(msgOffset), "events[1].payload.msg");
        assertEvent(command, events.poll(), EventType.COMMIT, 2, 0, "events[2]");
    }

    @Test
    public void commitEventNotAllowed() {
        //given
        final int input = 11;
        final long sequence = 22;
        final int type = 33;
        final long time = 44;
        command.init(new ExpandableArrayBuffer(), 0, input, sequence, type, time,
                new ExpandableArrayBuffer(12), 2, 10);
        final DirectBuffer zeroPayload = new ExpandableArrayBuffer(0);
        eventRouter.start(command);

        //when
        assertThrows(IllegalArgumentException.class,
                () -> eventRouter.routeEvent(EventType.COMMIT, zeroPayload, 0, 0)
        );
    }

    private void assertEvent(final Command command, final Event event,
                             final int type, final int index, final int payloadSize,
                             final String evtName) {
        assertEquals(command.id().input(), event.id().commandId().input(), evtName + ".id.commandId.input");
        assertEquals(command.id().sequence(), event.id().commandId().sequence(), evtName + ".id.commandId.sequence");
        assertEquals(index, event.id().index(), evtName + ".id.index");
        assertEquals(type, event.type(), evtName + ".type");
        assertEquals(command.time(), event.time(), evtName + ".time");
        assertEquals(payloadSize, event.payload().capacity(), evtName + ".payload.capacity");
    }
}