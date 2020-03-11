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
package org.tools4j.elara.loop;

import org.tools4j.nobark.loop.ExceptionHandler;
import org.tools4j.nobark.loop.IdleStrategy;
import org.tools4j.nobark.loop.Loop;
import org.tools4j.nobark.loop.Step;
import org.tools4j.nobark.run.StoppableThread;

import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

public class DutyCycle {

    private final Step[] steps;

    public DutyCycle(final SequencerStep sequencerStep,
                     final CommandPollerStep commandPollerStep,
                     final EventPollerStep eventPollerStep) {
        this.steps = steps(sequencerStep, commandPollerStep, eventPollerStep);
    }

    private Step[] steps(final SequencerStep sequencerStep,
                         final CommandPollerStep commandPollerStep,
                         final EventPollerStep eventPollerStep) {
        requireNonNull(sequencerStep);
        requireNonNull(commandPollerStep);
        requireNonNull(eventPollerStep);
        return new Step[] {sequencerStep, commandPollerStep, eventPollerStep};
    }

    /**
     * Creates, starts and returns a new thread running a loop with the duty cycle steps.
     *
     * @param idleStrategy      the strategy handling idle loop phases
     * @param exceptionHandler  the step exception handler
     * @param threadFactory     the factory to provide the service thread
     * @return the newly created and started thread running the loop
     */
    public StoppableThread start(final IdleStrategy idleStrategy,
                                 final ExceptionHandler exceptionHandler,
                                 final ThreadFactory threadFactory) {
        return Loop.start(idleStrategy, exceptionHandler, threadFactory, steps);
    }
}
