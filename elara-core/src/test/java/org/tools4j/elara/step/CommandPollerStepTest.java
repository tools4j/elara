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
package org.tools4j.elara.step;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tools4j.elara.store.MessageStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CommandPollerStep}
 */
@ExtendWith(MockitoExtension.class)
public class CommandPollerStepTest {

    @Mock
    private MessageStore.Poller commandPoller;
    @Mock
    private MessageStore.Handler handler;

    //under test
    private CommandPollerStep step;

    @BeforeEach
    public void init() {
        step = new CommandPollerStep(commandPoller, handler);
    }

    @Test
    public void pollIfAllEventsPolled() {
        //given
        final InOrder inOrder = inOrder(commandPoller);

        //when
        when(commandPoller.poll(any())).thenReturn(1);
        final int doneSome = step.doWork();

        //then
        assertTrue(doneSome > 0, "doneSome");
        inOrder.verify(commandPoller).poll(handler);
        inOrder.verifyNoMoreInteractions();

        //when
        when(commandPoller.poll(any())).thenReturn(0);
        final int doneNothing = step.doWork();

        //then
        assertEquals(0, doneNothing, "doneNothing");
        inOrder.verify(commandPoller).poll(handler);
        inOrder.verifyNoMoreInteractions();
    }
}