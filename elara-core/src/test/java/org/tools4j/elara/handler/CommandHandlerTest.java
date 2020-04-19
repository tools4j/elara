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
import org.tools4j.elara.flyweight.FlyweightEventRouter;
import org.tools4j.elara.log.PeekableMessageLog.PeekPollHandler.Result;
import org.tools4j.elara.plugin.base.BaseState;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CommandHandler}
 */
@ExtendWith(MockitoExtension.class)
public class CommandHandlerTest {

    @Mock
    private BaseState baseState;
    @Mock
    private CommandProcessor commandProcessor;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private DuplicateHandler duplicateHandler;
    @Mock
    private Command.Id commandId;
    @Mock
    private Command command;

    //under test
    private CommandHandler commandHandler;

    @BeforeEach
    public void init() {
        commandHandler = new CommandHandler(baseState, new FlyweightEventRouter(evt -> {}),
                commandProcessor, exceptionHandler, duplicateHandler);
        when(command.id()).thenReturn(commandId);
    }

    @Test
    public void commandSkippedIfAllEventsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq);
        result = commandHandler.onMessage(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verifyNoMoreInteractions();

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq + 1);
        result = commandHandler.onMessage(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessedIfNotAllEventsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.processCommands()).thenReturn(true);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 2);
        result = commandHandler.onMessage(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(same(command), notNull());
        inOrder.verify(duplicateHandler, never()).skipCommandProcessing(any());
        inOrder.verifyNoMoreInteractions();

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        result = commandHandler.onMessage(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(same(command), notNull());
        inOrder.verify(duplicateHandler, never()).skipCommandProcessing(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandPeekedIfCommandProcessingIsDisabled() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.processCommands()).thenReturn(false);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);
        Result result;

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        result = commandHandler.onMessage(command);

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
        final RuntimeException testException = new RuntimeException("test command processor exception");
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.processCommands()).thenReturn(true);
        final InOrder inOrder = inOrder(commandProcessor, exceptionHandler);
        Result result;

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        doThrow(testException).when(commandProcessor).onCommand(any(), any());
        result = commandHandler.onMessage(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor).onCommand(same(command), notNull());
        inOrder.verify(exceptionHandler).handleCommandProcessorException(command, testException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandSkipExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final RuntimeException testException = new RuntimeException("test skip command exception");
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler, exceptionHandler);
        Result result;

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq);
        doThrow(testException).when(duplicateHandler).skipCommandProcessing(any());
        result = commandHandler.onMessage(command);

        //then
        assertEquals(Result.POLL, result, "result");
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verify(exceptionHandler).handleCommandProcessorException(command, testException);
        inOrder.verifyNoMoreInteractions();
    }
}