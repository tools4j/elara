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
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.DefaultCommandLoopback;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.time.TimeSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ApplyingEventHandler}
 */
@ExtendWith(MockitoExtension.class)
public class ApplyingEventHandlerTest {

    @Mock
    private BaseState.Mutable baseState;
    @Mock
    private TimeSource timeSource;
    @Mock
    private SequenceGenerator adminSequenceGenerator;
    @Mock
    private MessageLog.Appender<Event> eventLogAppender;
    @Mock
    private Output output;
    @Mock
    private EventApplier eventApplier;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private DuplicateHandler duplicateHandler;
    @Mock
    private Command.Id commandId;
    @Mock
    private Event.Id eventId;
    @Mock
    private Event event;

    private CommandLoopback loopback;

    //under test
    private ApplyingEventHandler eventHandler;

    @BeforeEach
    public void init() {
        loopback =  new DefaultCommandLoopback(evt -> {}, timeSource, adminSequenceGenerator);
        eventHandler = new ApplyingEventHandler(baseState, loopback, eventLogAppender, output, eventApplier,
                exceptionHandler, duplicateHandler);
        when(eventId.commandId()).thenReturn(commandId);
        when(event.id()).thenReturn(eventId);
    }

    @Test
    public void eventSkippedIfCommandEventsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        final InOrder inOrder = inOrder(eventLogAppender, output, eventApplier, baseState, duplicateHandler);

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventLogAppender, never()).append(any());
        inOrder.verify(output, never()).publish(any(), anyBoolean(), any());
        inOrder.verify(eventApplier, never()).onEvent(any());
        inOrder.verify(baseState, never()).lastAppliedEvent(any());
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verifyNoMoreInteractions();

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq + 1);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventLogAppender, never()).append(any());
        inOrder.verify(output, never()).publish(any(), anyBoolean(), any());
        inOrder.verify(eventApplier, never()).onEvent(any());
        inOrder.verify(baseState, never()).lastAppliedEvent(any());
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventAppliedIfCommandEventsNotYetApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.allEventsPolled()).thenReturn(true);
        final InOrder inOrder = inOrder(eventLogAppender, output, eventApplier, baseState);

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 2);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventLogAppender).append(event);
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(baseState).lastAppliedEvent(event);
        inOrder.verify(output).publish(event, false, loopback);
        inOrder.verifyNoMoreInteractions();

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventLogAppender).append(event);
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(baseState).lastAppliedEvent(event);
        inOrder.verify(baseState, never()).allEventsAppliedFor(any());
        inOrder.verify(output).publish(event, false, loopback);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void allEventsAppliedWhenCommitEventIsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.allEventsPolled()).thenReturn(true);
        final InOrder inOrder = inOrder(eventLogAppender, output, eventApplier, baseState);

        //when
        when(event.type()).thenReturn(EventType.COMMIT);
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventLogAppender).append(event);
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(baseState).lastAppliedEvent(event);
        inOrder.verify(baseState).allEventsAppliedFor(commandId);
        inOrder.verify(output).publish(event, false, loopback);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventAppliedAndPublishedAsReplayIfNotAllEventsPolled() {
        //given
        final int input = 1;
        final long seq = 22;
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.allEventsPolled()).thenReturn(false);
        final InOrder inOrder = inOrder(eventLogAppender, output, eventApplier, baseState);

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventLogAppender, never()).append(any());
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(baseState).lastAppliedEvent(event);
        inOrder.verify(output).publish(event, true, loopback);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventApplierExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final RuntimeException testException = new RuntimeException("test event applier exception");
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        final InOrder inOrder = inOrder(eventApplier, exceptionHandler);

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        doThrow(testException).when(eventApplier).onEvent(any());
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(exceptionHandler).handleEventApplierException(event, testException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventOutputExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final RuntimeException testException = new RuntimeException("test event output exception");
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        when(baseState.allEventsPolled()).thenReturn(true);
        final InOrder inOrder = inOrder(output, eventApplier, exceptionHandler);

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq - 1);
        doThrow(testException).when(output).publish(any(), anyBoolean(), any());
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(output).publish(event, false, loopback);
        inOrder.verify(exceptionHandler).handleEventOutputException(event, testException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventSkipExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final RuntimeException testException = new RuntimeException("test skip event exception");
        when(commandId.input()).thenReturn(input);
        when(commandId.sequence()).thenReturn(seq);
        final InOrder inOrder = inOrder(eventApplier, duplicateHandler, exceptionHandler);

        //when
        when(baseState.lastCommandAllEventsApplied(input)).thenReturn(seq + 1);
        doThrow(testException).when(duplicateHandler).skipEventApplying(any());
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier, never()).onEvent(same(event));
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verify(exceptionHandler).handleEventApplierException(event, testException);
        inOrder.verifyNoMoreInteractions();
    }
}