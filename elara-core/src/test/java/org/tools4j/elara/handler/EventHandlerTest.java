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
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.Flags;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.DefaultCommandLoopback;
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
 * Unit test for {@link EventHandler}
 */
@ExtendWith(MockitoExtension.class)
public class EventHandlerTest {

    @Mock
    private BaseState.Mutable baseState;
    @Mock
    private MessageLog.Appender commandAppender;
    @Mock
    private TimeSource timeSource;
    @Mock
    private SequenceGenerator adminSequenceGenerator;
    @Mock
    private Output output;
    @Mock
    private EventApplier eventApplier;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private DuplicateHandler duplicateHandler;

    private CommandLoopback loopback;

    //under test
    private EventHandler eventHandler;

    @BeforeEach
    public void init() {
        loopback =  new DefaultCommandLoopback(commandAppender, timeSource, adminSequenceGenerator);
        eventHandler = new EventHandler(baseState, loopback, output, eventApplier, exceptionHandler, duplicateHandler);
    }

    @Test
    public void eventSkippedIfCommandEventsApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        final short index = 2;
        final Event event = event(input, seq, index);
        final InOrder inOrder = inOrder(output, eventApplier, baseState, duplicateHandler);

        //when
        when(baseState.eventApplied(event.id())).thenReturn(true);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(output, never()).publish(any(), anyBoolean(), any());
        inOrder.verify(eventApplier, never()).onEvent(any());
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventAppliedIfCommandEventsNotYetApplied() {
        //given
        final int input = 1;
        final long seq = 22;
        final short index = 2;
        final Event event = event(input, seq, index);
        when(baseState.allEventsPolled()).thenReturn(true);
        final InOrder inOrder = inOrder(output, eventApplier, baseState);

        //when
        when(baseState.eventApplied(event.id())).thenReturn(false);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(output).publish(event, false, loopback);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventAppliedAndPublishedAsReplayIfNotAllEventsPolled() {
        //given
        final int input = 1;
        final long seq = 22;
        final short index = 2;
        final Event event = event(input, seq, index);
        when(baseState.allEventsPolled()).thenReturn(false);
        final InOrder inOrder = inOrder(output, eventApplier, baseState);

        //when
        when(baseState.eventApplied(event.id())).thenReturn(false);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(output).publish(event, true, loopback);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventApplierExceptionInvokesErrorHandler() {
        //given
        final int input = 1;
        final long seq = 22;
        final short index = 2;
        final Event event = event(input, seq, index);
        final RuntimeException testException = new RuntimeException("test event applier exception");
        final InOrder inOrder = inOrder(eventApplier, exceptionHandler);

        //when
        when(baseState.eventApplied(event.id())).thenReturn(false);
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
        final short index = 2;
        final Event event = event(input, seq, index);
        final RuntimeException testException = new RuntimeException("test event output exception");
        when(baseState.allEventsPolled()).thenReturn(true);
        final InOrder inOrder = inOrder(output, eventApplier, exceptionHandler);

        //when
        when(baseState.eventApplied(event.id())).thenReturn(false);
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
        final short index = 2;
        final Event event = event(input, seq, index);
        final RuntimeException testException = new RuntimeException("test skip event exception");
        final InOrder inOrder = inOrder(eventApplier, duplicateHandler, exceptionHandler);

        //when
        when(baseState.eventApplied(event.id())).thenReturn(true);
        doThrow(testException).when(duplicateHandler).skipEventApplying(any());
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier, never()).onEvent(same(event));
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verify(exceptionHandler).handleEventApplierException(event, testException);
        inOrder.verifyNoMoreInteractions();
    }

    private static Event event(final int input, final long seq, final short index) {
        return event(input, seq, index, EventType.APPLICATION);
    }
    private static Event event(final int input, final long seq, final short index, final int type) {
        return new FlyweightEvent()
                .init(new ExpandableArrayBuffer(), 0, input, seq, index, type, 123L,
                        Flags.NONE, new UnsafeBuffer(0, 0), 0, 0
                );
    }
}