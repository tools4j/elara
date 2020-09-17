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

import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultCommandHandler}
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
    private DefaultCommandHandler commandHandler;

    @BeforeEach
    public void init() {
        commandHandler = new DefaultCommandHandler(baseState, eventRouter, commandProcessor, exceptionHandler,
                duplicateHandler);
    }

    @Test
    public void commandSkippedIfAllEventsApplied() {
        //given
        final int source = 1;
        final long seq = 22;
        final Command command = command(source, seq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(true);
        result = commandHandler.onCommand(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessedIfNotAllEventsApplied() {
        //given
        final int source = 1;
        final long seq = 22;
        final Command command = command(source, seq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(false);
        result = commandHandler.onCommand(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(eq(command), notNull());
        inOrder.verify(duplicateHandler, never()).skipCommandProcessing(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessorExceptionInvokesErrorHandler() {
        //given
        final int source = 1;
        final long seq = 22;
        final Command command = command(source, seq);
        final RuntimeException testException = new RuntimeException("test command processor exception");
        final InOrder inOrder = inOrder(commandProcessor, exceptionHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(false);
        doThrow(testException).when(commandProcessor).onCommand(any(), any());
        result = commandHandler.onCommand(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(eq(command), notNull());
        inOrder.verify(exceptionHandler).handleCommandProcessorException(command, testException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandSkipExceptionInvokesErrorHandler() {
        //given
        final int source = 1;
        final long seq = 22;
        final Command command = command(source, seq);
        final RuntimeException testException = new RuntimeException("test skip command exception");
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler, exceptionHandler);
        Result result;

        //when
        when(baseState.allEventsAppliedFor(notNull())).thenReturn(true);
        doThrow(testException).when(duplicateHandler).skipCommandProcessing(any());
        result = commandHandler.onCommand(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verify(exceptionHandler).handleCommandProcessorException(command, testException);
        inOrder.verifyNoMoreInteractions();
    }

    private static Command command(final int source, final long seq) {
        return new FlyweightCommand()
                .init(new ExpandableArrayBuffer(), 0, source, seq, EventType.APPLICATION, 123L,
                        new UnsafeBuffer(0, 0), 0, 0
                );
    }
}