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
package org.tools4j.elara.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.tools4j.elara.app.state.DefaultBaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.route.DefaultEventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.store.InMemoryStore;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.store.MessageStore.Poller;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.flyweight.EventType.APP_COMMIT;
import static org.tools4j.elara.flyweight.EventType.AUTO_COMMIT;
import static org.tools4j.elara.flyweight.EventType.INTERMEDIARY;

/**
 * Unit test for {@link DefaultEventRouter}
 */
public class DefaultEventRouterTest {

    private MutableBaseState baseState;
    private MessageStore messageStore;
    private List<Event> routed;
    private FlyweightCommand command;
    private long eventTime;

    //under test
    private DefaultEventRouter eventRouter;

    @BeforeEach
    public void init() {
        baseState = new DefaultBaseState();
        messageStore = new InMemoryStore();
        routed = new ArrayList<>();
        command = new FlyweightCommand();
        eventRouter = new DefaultEventRouter(() -> eventTime, baseState, messageStore.appender(), event -> {
            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
            event.writeTo(buffer, 0);
            routed.add(new FlyweightEvent().wrap(buffer, 0));
            baseState.applyEvent(event);
        });
    }

    @Test
    public void noEvents() {
        //given
        final int sourceIdId = 11;
        final long sourceIdSeq = 22;
        final int type = 33;
        final long time = 44;
        eventTime = time + 1;

        //when
        startWithCommand(sourceIdId, sourceIdSeq, type, time);

        //then
        assertEquals(0, eventRouter.nextEventSequence(), "nextEventSequence[0]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[0]");

        //when
        eventRouter.complete();

        //then
        assertEquals(1, eventRouter.nextEventSequence(), "nextEventSequence[x]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[x]");
        assertEquals(1, routed.size(), "routed.size");
        assertEvent(command, routed.get(0), AUTO_COMMIT, 0, 0, BaseEvents.AUTO_COMMIT, 0, "events[0]");
        assertEvent(command, poll(0), AUTO_COMMIT, 0, 0, BaseEvents.AUTO_COMMIT, 0, "poll(0)");
    }

    @Test
    public void someEvents() {
        //given
        final int sourceIdId = 11;
        final long sourceIdSeq = 22;
        final int type = 33;
        final long time = 44;
        final int eventType = 55;
        final int msgOffset = 2;
        final String msg = "Hello world";
        eventTime = time + 1;

        startWithCommand(sourceIdId, sourceIdSeq, type, time);

        //when
        routeEmptyApplicationEvent();

        //then
        assertEquals(1, eventRouter.nextEventSequence(), "nextEventSequence[1]");
        assertEquals(1, eventRouter.nextEventIndex(), "nextEventIndex[1]");

        //when
        final int payloadSize = routeEvent(eventType, msgOffset,  msg);

        //then
        assertEquals(2, eventRouter.nextEventSequence(), "nextEventSequence[2]");
        assertEquals(2, eventRouter.nextEventIndex(), "nextEventIndex[2]");

        //when
        eventRouter.complete();

        //then
        assertEquals(2, eventRouter.nextEventSequence(), "nextEventSequence[x]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[x]");
        assertEquals(2, routed.size(), "routed.size");
        assertEvent(command, routed.get(0), INTERMEDIARY, 0, 0, PayloadType.DEFAULT, 0, "events[0]");
        assertEvent(command, routed.get(1), INTERMEDIARY, 1, 1, eventType, payloadSize, "events[1]");
        assertEvent(command, poll(1), APP_COMMIT, 1, 1, eventType, payloadSize, "poll(1)");
        assertEquals(msg, routed.get(1).payload().getStringAscii(msgOffset), "routed[1].payload.msg");
    }

    @Test
    public void lastEventAborted() {
        //given
        final int sourceIdId = 11;
        final long sourceIdSeq = 22;
        final int type = 33;
        final long time = 44;
        eventTime = time + 1;

        startWithCommand(sourceIdId, sourceIdSeq, type, time);

        //when
        routeEmptyApplicationEvent();

        //then
        assertEquals(1, eventRouter.nextEventSequence(), "nextEventSequence[1]");
        assertEquals(1, eventRouter.nextEventIndex(), "nextEventIndex[1]");

        //when
        try (RoutingContext context = eventRouter.routingEvent()) {
            context.buffer().putInt(0, 123);
            context.buffer().putStringAscii(4, "Hello world");
            context.abort();
        }

        //then
        assertEquals(1, eventRouter.nextEventSequence(), "nextEventSequence[1+-]");
        assertEquals(1, eventRouter.nextEventIndex(), "nextEventIndex[1+-]");

        //when
        eventRouter.complete();

        //then
        assertEquals(2, eventRouter.nextEventSequence(), "nextEventSequence[x]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[x]");
        assertEquals(2, routed.size(), "routed.size");
        assertEvent(command, routed.get(0), INTERMEDIARY, 0, 0, PayloadType.DEFAULT, 0, "events[0]");
        assertEvent(command, routed.get(1), AUTO_COMMIT, 1, 1, BaseEvents.AUTO_COMMIT, 0, "events[1]");
        assertEvent(command, poll(1), AUTO_COMMIT, 1, 1, BaseEvents.AUTO_COMMIT, 0, "poll(1)");
    }

    @Test
    public void skipCommand() {
        //given
        final int sourceId = 11;
        final long sourceSeq = 22;
        final int type = 33;
        final long time = 44;
        eventTime = time + 1;

        startWithCommand(sourceId, sourceSeq, type, time);

        //when
        final boolean skipped = eventRouter.skipCommand();
        eventRouter.complete();

        //then
        assertTrue(skipped, "skipped");
        assertEquals(0, eventRouter.nextEventSequence(), "nextEventSequence[x]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[x]");
        assertEquals(0, routed.size(), "routed.size");
    }

    @Test
    public void skipCommandDuringEventRouting() {
        //given
        final int sourceId = 11;
        final long sourceSeq = 22;
        final int type = 33;
        final long time = 44;
        eventTime = time + 1;

        startWithCommand(sourceId, sourceSeq, type, time);

        //when
        final boolean skipped;
        try (final RoutingContext routingContext = eventRouter.routingEvent()) {
            routingContext.buffer().putStringAscii(0, "Hello world");
            skipped = eventRouter.skipCommand();
        }
        eventRouter.complete();

        //then
        assertTrue(skipped, "skipped");
        assertEquals(0, eventRouter.nextEventSequence(), "nextEventSequence[x]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[x]");
        assertEquals(0, routed.size(), "routed.size");
    }

    @Test
    public void skipCommandNotPossible() {
        //given
        final int sourceId = 11;
        final long sourceSeq = 22;
        final int type = 33;
        final long time = 44;
        final int eventType = 55;
        final int msgOffset = 2;
        final String msg = "Hello world";
        eventTime = time + 1;

        startWithCommand(sourceId, sourceSeq, type, time);
        routeEmptyApplicationEvent();
        final int payloadSize = routeEvent(eventType, msgOffset, msg);

        //when
        final boolean skipped = eventRouter.skipCommand();
        eventRouter.complete();

        //then
        assertFalse(skipped, "skipped");
        assertEquals(2, eventRouter.nextEventSequence(), "nextEventSequence[x]");
        assertEquals(0, eventRouter.nextEventIndex(), "nextEventIndex[x]");
        assertEquals(2, routed.size(), "routed.size");
        assertEvent(command, routed.get(0), INTERMEDIARY, 0, 0, PayloadType.DEFAULT, 0, "events[0]");
        assertEvent(command, routed.get(1), INTERMEDIARY, 1, 1, eventType, payloadSize, "events[1]");
        assertEvent(command, poll(1), APP_COMMIT, 1, 1, eventType, payloadSize, "poll(1)");
    }

    @Test
    public void skipCommandPreventsEventRouting() {
        //given
        final int sourceId = 11;
        final long sourceSeq = 22;
        final int type = 33;
        final long time = 44;

        startWithCommand(sourceId, sourceSeq, type, time);
        final boolean skipped = eventRouter.skipCommand();
        assertTrue(skipped, "skipped");
        eventTime = time + 1;

        //when + then
        assertThrows(IllegalStateException.class, this::routeEmptyApplicationEvent);
    }

    private void startWithCommand(final int sourceId,
                                  final long sourceSeq,
                                  final int type,
                                  final long time) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        FlyweightCommand.writeHeader(sourceId, sourceSeq, time, type, 10, buffer, 0);
        command.wrap(buffer, 0);
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

    private Event poll(final int index) {
        final Poller poller = messageStore.poller();
        //skip those we are not interested in
        for (int i = 0; i < index; i++) {
            poller.poll(message -> Result.POLL);
        }
        //read and copy the event we actually want
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        poller.poll(message -> {
            buffer.putBytes(0, message, 0, message.capacity());
            return Result.POLL;
        });
        return new FlyweightEvent().wrap(buffer, 0);
    }

    private void assertEvent(final Command command, final Event event,
                             final EventType eventType, final long eventSequence, final int index,
                             final int payloadType, final int payloadSize, final String evtName) {
        assertEquals(command.sourceId(), event.sourceId(), evtName + ".sourceId");
        assertEquals(command.sourceSequence(), event.sourceSequence(), evtName + ".sourceSequence");
        assertEquals(eventType, event.eventType(), evtName + ".eventType");
        assertEquals(eventSequence, event.eventSequence(), evtName + ".eventSequence");
        assertEquals(index, event.eventIndex(), evtName + ".index");
        assertEquals(eventTime, event.eventTime(), evtName + ".eventTime");
        assertEquals(eventTime, event.eventTime(), evtName + ".eventTime");
        assertEquals(payloadType, event.payloadType(), evtName + ".payloadType");
        assertEquals(payloadSize, event.payload().capacity(), evtName + ".payload.capacity");
    }
}