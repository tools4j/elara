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
package org.tools4j.elara.app.state;

import org.junit.jupiter.api.Test;
import org.tools4j.elara.time.TimeSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.tools4j.elara.flyweight.EventType.AUTO_COMMIT;
import static org.tools4j.elara.flyweight.EventType.INTERMEDIARY;

/**
 * Unit test for {@link DefaultEventState}
 */
class DefaultEventStateTest {

    @Test
    void nilDefaultValues() {
        //given
        final int sourceId = 123;
        final EventState eventState = new DefaultEventState(sourceId);

        //when + then
        assertNilValues(sourceId, eventState);
    }

    @Test
    void lastEventValues() {
        //given
        final int sourceId = 123;
        final DefaultEventState eventState = new DefaultEventState(sourceId);


        //when
        eventState.applyEvent(22L, 33L, 0, INTERMEDIARY, 44L, 55);

        //then
        assertEquals(sourceId, eventState.sourceId(), "sourceId");
        assertEquals(1, eventState.eventsProcessed(), "eventsProcessed");
        assertEquals(22L, eventState.sourceSequence(), "sourceSequence");
        assertEquals(33L, eventState.eventSequence(), "eventSequence");
        assertEquals(0, eventState.eventIndex(), "eventIndex");
        assertEquals(INTERMEDIARY, eventState.eventType(), "eventType");
        assertEquals(44L, eventState.eventTime(), "eventTime");
        assertEquals(55, eventState.payloadType(), "payloadType");

        //when
        eventState.applyEvent(23L, 34L, 1, AUTO_COMMIT, 45L, 56);

        //then
        assertEquals(sourceId, eventState.sourceId(), "sourceId");
        assertEquals(2, eventState.eventsProcessed(), "eventsProcessed");
        assertEquals(23L, eventState.sourceSequence(), "sourceSequence");
        assertEquals(34L, eventState.eventSequence(), "eventSequence");
        assertEquals(1, eventState.eventIndex(), "eventIndex");
        assertEquals(AUTO_COMMIT, eventState.eventType(), "eventType");
        assertEquals(45L, eventState.eventTime(), "eventTime");
        assertEquals(56, eventState.payloadType(), "payloadType");
    }

    static void assertNilValues(final int sourceId, final EventState eventState) {
        assertEquals(sourceId, eventState.sourceId(), "sourceId");
        assertEquals(0, eventState.eventsProcessed(), "eventsProcessed");
        assertEquals(BaseState.NIL_SEQUENCE, eventState.sourceSequence(), "sourceSequence");
        assertEquals(BaseState.NIL_SEQUENCE, eventState.eventSequence(), "eventSequence");
        assertEquals(-1, eventState.eventIndex(), "eventIndex");
        assertNull(eventState.eventType(), "eventType");
        assertEquals(TimeSource.MIN_VALUE, eventState.eventTime(), "eventTime");
        assertEquals(0, eventState.payloadType(), "payloadType");
    }
}