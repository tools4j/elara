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
package org.tools4j.elara.handler;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.log.MessageLog.Handler.Result;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.route.DefaultEventRouter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ProcessingCommandHandler}
 */
@ExtendWith(MockitoExtension.class)
public class CommandHandlerTest {

    @Mock
    private BaseState baseState;
    @Mock
    private DefaultEventRouter eventRouter;
    @Mock
    private CommandProcessor commandProcessor;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private DuplicateHandler duplicateHandler;

    //under test
    private ProcessingCommandHandler commandHandler;

    @BeforeEach
    public void init() {
        commandHandler = new ProcessingCommandHandler(baseState, eventRouter, commandProcessor, exceptionHandler,
                duplicateHandler);
    }

    @Test
    public void commandSkippedIfAllEventsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        final Command command = command(input, seq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(true);
        result = commandHandler.onMessage(toDirectBuffer(command));

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(eqCommand(command));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessedIfNotAllEventsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        final Command command = command(input, seq);
        when(baseState.processCommands()).thenReturn(true);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(false);
        result = commandHandler.onMessage(toDirectBuffer(command));

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(eqCommand(command), notNull());
        inOrder.verify(duplicateHandler, never()).skipCommandProcessing(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandPeekedIfCommandProcessingIsDisabled() {
        //given
        final int input = 1;
        final long seq = 22;
        final Command command = command(input, seq);
        when(baseState.processCommands()).thenReturn(false);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(false);
        result = commandHandler.onMessage(toDirectBuffer(command));

        //then
        assertEquals(Result.PEEK, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler, never()).skipCommandProcessing(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessorExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final Command command = command(input, seq);
        final RuntimeException testException = new RuntimeException("test command processor exception");
        when(baseState.processCommands()).thenReturn(true);
        final InOrder inOrder = inOrder(commandProcessor, exceptionHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(false);
        doThrow(testException).when(commandProcessor).onCommand(any(), any());
        result = commandHandler.onMessage(toDirectBuffer(command));

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(eqCommand(command), notNull());
        inOrder.verify(exceptionHandler).handleCommandProcessorException(eqCommand(command), same(testException));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandSkipExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final Command command = command(input, seq);
        final RuntimeException testException = new RuntimeException("test skip command exception");
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler, exceptionHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(true);
        doThrow(testException).when(duplicateHandler).skipCommandProcessing(any());
        result = commandHandler.onMessage(toDirectBuffer(command));

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(eqCommand(command));
        inOrder.verify(exceptionHandler).handleCommandProcessorException(eqCommand(command), same(testException));
        inOrder.verifyNoMoreInteractions();
    }

    private static Command eqCommand(final Command command) {
        final String exp ="eq[command=" + command + "]";
        final DirectBuffer raw = toDirectBuffer(command);
        return Mockito.argThat(new ArgumentMatcher<Command>() {
            @Override
            public boolean matches(final Command argument) {
                return 0 == raw.compareTo(toDirectBuffer(argument));
            }

            @Override
            public String toString() {
                return exp;
            }
        });
    }

    private static Command command(final int input, final long seq) {
        return new FlyweightCommand()
                .init(new ExpandableArrayBuffer(), 0, input, seq, EventType.APPLICATION, 123L,
                        new UnsafeBuffer(0, 0), 0, 0
                );
    }

    private static DirectBuffer toDirectBuffer(final Command command) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        command.writeTo(buffer, 0);
        return buffer;
    }
}