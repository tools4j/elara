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
package org.tools4j.elara.handler;

import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.flyweight.PayloadType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultEventHandler}
 */
@ExtendWith(MockitoExtension.class)
public class DefaultEventHandlerTest {

    @Mock
    private MutableBaseState baseState;
    @Mock
    private EventApplier eventApplier;
    @Mock
    private ExceptionHandler exceptionHandler;
    @Mock
    private DuplicateHandler duplicateHandler;

    //under test
    private EventHandler eventHandler;

    @BeforeEach
    public void init() {
        eventHandler = new DefaultEventHandler(baseState, eventApplier, exceptionHandler, duplicateHandler);
    }

    @Test
    public void eventSkippedIfAlreadyApplied() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final long eventSeq = 444;
        final short index = 2;
        final Event event = event(sourceId, sourceSeq, eventSeq, index);
        final InOrder inOrder = inOrder(eventApplier, baseState, duplicateHandler);

        //when
        when(baseState.eventApplied(eventSeq)).thenReturn(true);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier, never()).onEvent(any());
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventAppliedIfNotYetApplied() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final long eventSeq = 444;
        final short index = 2;
        final Event event = event(sourceId, sourceSeq, eventSeq, index);
        final InOrder inOrder = inOrder(eventApplier, baseState);

        //when
        when(baseState.eventApplied(eventSeq)).thenReturn(false);
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(baseState).applyEvent(same(event));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventApplierExceptionInvokesErrorHandler() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final long eventSeq = 444;
        final short index = 2;
        final Event event = event(sourceId, sourceSeq, eventSeq, index);
        final RuntimeException testException = new RuntimeException("test event applier exception");
        final InOrder inOrder = inOrder(eventApplier, exceptionHandler);

        //when
        when(baseState.eventApplied(eventSeq)).thenReturn(false);
        doThrow(testException).when(eventApplier).onEvent(any());
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier).onEvent(same(event));
        inOrder.verify(exceptionHandler).handleEventApplierException(event, testException);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void eventSkipExceptionInvokesErrorHandler() {
        //given
        final int sourceId = 1;
        final long sourceSeq = 22;
        final long eventSeq = 444;
        final short index = 2;
        final Event event = event(sourceId, sourceSeq, eventSeq, index);
        final RuntimeException testException = new RuntimeException("test skip event exception");
        final InOrder inOrder = inOrder(eventApplier, duplicateHandler, exceptionHandler);

        //when
        when(baseState.eventApplied(eventSeq)).thenReturn(true);
        doThrow(testException).when(duplicateHandler).skipEventApplying(any());
        eventHandler.onEvent(event);

        //then
        inOrder.verify(eventApplier, never()).onEvent(same(event));
        inOrder.verify(duplicateHandler).skipEventApplying(event);
        inOrder.verify(exceptionHandler).handleEventApplierException(event, testException);
        inOrder.verifyNoMoreInteractions();
    }

    private static Event event(final int sourceId, final long sourceSeq, final long eventSeq, final short index) {
        return event(sourceId, sourceSeq, eventSeq, index, PayloadType.DEFAULT);
    }
    private static Event event(final int sourceId, final long sourceSeq, final long eventSeq, final short index, final int payloadType) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(FlyweightEvent.HEADER_LENGTH);
        FlyweightEvent.writeHeader(
                EventType.INTERMEDIARY, sourceId, sourceSeq, index, eventSeq,123L, payloadType, 0, buffer, 0
        );
        return new FlyweightEvent().wrapSilently(buffer, 0);
    }
}