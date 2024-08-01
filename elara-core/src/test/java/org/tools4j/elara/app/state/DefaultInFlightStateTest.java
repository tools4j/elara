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
package org.tools4j.elara.app.state;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.tools4j.elara.flyweight.EventType;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.tools4j.elara.flyweight.CommandFrame.HEADER_LENGTH;

/**
 * Unit test for {@link DefaultInFlightState}.
 */
class DefaultInFlightStateTest {

    @MethodSource("inFlightStateInstances")
    @ParameterizedTest(name = "{0}")
    void commandsSentAndEventsReceived(final String name, final DefaultInFlightState state) {
        //when
        state.onCommandSent(1, 1, 300, 100);
        state.onCommandSent(2, 1, 301, 200);

        //then
        assertEquals(2, state.inFlightCommands());
        assertEquals(1, state.inFlightCommands(1));
        assertEquals(1, state.inFlightCommands(2));
        assertEquals(0, state.inFlightCommands(3));
        assertEquals(300 + 2*HEADER_LENGTH, state.inFlightBytes());
        assertTrue(state.hasInFlightCommand());
        assertTrue(state.hasInFlightCommand(1));
        assertTrue(state.hasInFlightCommand(2));
        assertFalse(state.hasInFlightCommand(3));

        //when
        state.onEvent(1, 1, 1001, 0, EventType.APP_COMMIT, 0, 0, 100);
        state.onCommandSent(1, 2, 302, 150);

        //then
        assertEquals(2, state.inFlightCommands());
        assertEquals(1, state.inFlightCommands(1));
        assertEquals(1, state.inFlightCommands(2));
        assertEquals(350 + 2*HEADER_LENGTH, state.inFlightBytes());
        assertTrue(state.hasInFlightCommand());
        assertTrue(state.hasInFlightCommand(1));
        assertTrue(state.hasInFlightCommand(2));

        //when
        state.onCommandSent(1, 3, 303, 30);
        state.onCommandSent(1, 4, 304, 0);
        state.onEvent(2, 1, 0, 1002, EventType.APP_COMMIT, 0, 0, 200);

        //then
        assertEquals(3, state.inFlightCommands());
        assertEquals(3, state.inFlightCommands(1));
        assertEquals(0, state.inFlightCommands(2));
        assertEquals(180 + 3*HEADER_LENGTH, state.inFlightBytes());
        assertTrue(state.hasInFlightCommand());
        assertTrue(state.hasInFlightCommand(1));
        assertFalse(state.hasInFlightCommand(2));
        assertEquals(1, state.sourceId(0));
        assertEquals(2, state.sourceSequence(0));
        assertEquals(302, state.sendingTime(0));
        assertEquals(1, state.sourceId(1));
        assertEquals(3, state.sourceSequence(1));
        assertEquals(303, state.sendingTime(1));
        assertEquals(1, state.sourceId(2));
        assertEquals(4, state.sourceSequence(2));
        assertEquals(304, state.sendingTime(2));

        //when
        state.onCommandSent(2, 2, 400, 40);
        state.onCommandSent(3, 1, 401, 5);
        state.onEvent(1, 2, 1003, 0, EventType.APP_COMMIT, 0, 0, 150);

        //then
        assertEquals(4, state.inFlightCommands());
        assertEquals(2, state.inFlightCommands(1));
        assertEquals(1, state.inFlightCommands(2));
        assertEquals(1, state.inFlightCommands(3));
        assertEquals(75 + 4*HEADER_LENGTH, state.inFlightBytes());
        assertTrue(state.hasInFlightCommand());
        assertTrue(state.hasInFlightCommand(1));
        assertTrue(state.hasInFlightCommand(2));
        assertTrue(state.hasInFlightCommand(3));

        //when
        state.onEvent(1, 3, 1004, 0, EventType.APP_COMMIT, 0, 0, 30);
        state.onEvent(1, 4, 1005, 0, EventType.APP_COMMIT, 0, 0, 0);

        //then
        assertEquals(2, state.inFlightCommands());
        assertEquals(0, state.inFlightCommands(1));
        assertEquals(1, state.inFlightCommands(2));
        assertEquals(1, state.inFlightCommands(3));
        assertEquals(45 + 2*HEADER_LENGTH, state.inFlightBytes());
        assertTrue(state.hasInFlightCommand());
        assertFalse(state.hasInFlightCommand(1));
        assertTrue(state.hasInFlightCommand(2));
        assertTrue(state.hasInFlightCommand(3));
        assertEquals(2, state.sourceId(0));
        assertEquals(2, state.sourceSequence(0));
        assertEquals(400, state.sendingTime(0));
        assertEquals(3, state.sourceId(1));
        assertEquals(1, state.sourceSequence(1));
        assertEquals(401, state.sendingTime(1));

        //when
        state.reset();

        //then
        assertEquals(0, state.inFlightCommands());
        assertEquals(0, state.inFlightCommands(1));
        assertEquals(0, state.inFlightCommands(2));
        assertEquals(0, state.inFlightCommands(3));
        assertEquals(0, state.inFlightBytes());
        assertFalse(state.hasInFlightCommand());
        assertFalse(state.hasInFlightCommand(1));
        assertFalse(state.hasInFlightCommand(2));
        assertFalse(state.hasInFlightCommand(3));
    }

    public static Stream<Arguments> inFlightStateInstances() {
        return Stream.of(
                Arguments.of("DefaultInFlightState[capacity=100]", new DefaultInFlightState(100, 100)),
                Arguments.of("DefaultInFlightState[capacity=1]", new DefaultInFlightState(1, 1)),
                Arguments.of("DefaultInFlightState[capacity=2]", new DefaultInFlightState(2, 2)),
                Arguments.of("DefaultInFlightState[capacity=3]", new DefaultInFlightState(3, 3)),
                Arguments.of("DefaultInFlightState[capacity=4]", new DefaultInFlightState(4, 4))
        );
    }

}