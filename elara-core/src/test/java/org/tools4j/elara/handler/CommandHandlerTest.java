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
package org.tools4j.elara.handler;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.PayloadType;
import org.tools4j.elara.route.CommandTransaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DeduplicatingCommandHandler} and {@link ProcessingCommandHandler}.
 */
@ExtendWith(MockitoExtension.class)
public class CommandHandlerTest {

    @Mock
    private BaseState baseState;
    @Mock(answer = Answers.RETURNS_MOCKS)
    private CommandTransaction commandTransaction;
    @Mock
    private CommandProcessor commandProcessor;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private DuplicateHandler duplicateHandler;

    //under test
    private CommandHandler commandHandler;

    @BeforeEach
    public void init() {
        commandHandler = new DeduplicatingCommandHandler(
                baseState, new ProcessingCommandHandler(commandTransaction, commandProcessor), exceptionHandler,
                duplicateHandler);
    }

    @Test
    public void commandSkippedIfEventAlreadyAppliedForCommand() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final Command command = command(sourceId, sourceSeq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);

        //when
        when(baseState.eventAppliedForCommand(anyInt(), anyLong())).thenReturn(true);
        commandHandler.onCommand(command);

        //then
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessedIfEventNotYetAppliedForCommand() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final Command command = command(sourceId, sourceSeq);
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler);

        //when
        when(baseState.eventAppliedForCommand(anyInt(), anyLong())).thenReturn(false);
        commandHandler.onCommand(command);

        //then
        inOrder.verify(commandProcessor).onCommand(eq(command), notNull());
        inOrder.verify(duplicateHandler, never()).skipCommandProcessing(any());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandProcessorExceptionInvokesErrorHandler() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final Command command = command(sourceId, sourceSeq);
        final RuntimeException testException = new RuntimeException("test command processor exception");
        final InOrder inOrder = inOrder(commandProcessor, exceptionHandler);

        //when
        when(baseState.eventAppliedForCommand(anyInt(), anyLong())).thenReturn(false);
        doThrow(testException).when(commandProcessor).onCommand(any(), any());
        commandHandler.onCommand(command);

        //then
        inOrder.verify(commandProcessor).onCommand(eq(command), notNull());
        inOrder.verify(exceptionHandler).handleCommandProcessorException(command, testException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void commandSkipExceptionInvokesErrorHandler() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final Command command = command(sourceId, sourceSeq);
        final RuntimeException testException = new RuntimeException("test skip command exception");
        final InOrder inOrder = inOrder(commandProcessor, duplicateHandler, exceptionHandler);

        //when
        when(baseState.eventAppliedForCommand(anyInt(), anyLong())).thenReturn(true);
        doThrow(testException).when(duplicateHandler).skipCommandProcessing(any());
        commandHandler.onCommand(command);

        //then
        inOrder.verify(commandProcessor, never()).onCommand(any(), any());
        inOrder.verify(duplicateHandler).skipCommandProcessing(command);
        inOrder.verify(exceptionHandler).handleCommandProcessorException(command, testException);
        inOrder.verifyNoMoreInteractions();
    }

    private static Command command(final int sourceId, final long sourceSeq) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        FlyweightCommand.writeHeader(sourceId, sourceSeq, 123L, 0, PayloadType.DEFAULT, buffer, 0);
        return new FlyweightCommand().wrap(buffer, 0);
    }
}