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

public class DutyCycleStep implements Step {

    private final SequencerStep sequencerStep;
    private final CommandPollerStep commandPollerStep;
    private final EventPollerStep eventPollerStep;
    private final Step outputStep;

    public DutyCycleStep(final SequencerStep sequencerStep,
                         final CommandPollerStep commandPollerStep,
                         final EventPollerStep eventPollerStep,
                         final Step outputStep) {
        this.sequencerStep = requireNonNull(sequencerStep);
        this.commandPollerStep = requireNonNull(commandPollerStep);
        this.eventPollerStep = requireNonNull(eventPollerStep);
        this.outputStep = requireNonNull(outputStep);
    }


    @Override
    public boolean perform() {
        if (eventPollerStep.perform()) {
            outputStep.perform();
            return true;
        }
        if (commandPollerStep.perform()) {
            outputStep.perform();
            return true;
        }
        //NOTES:
        //   1. we don't poll inputs during replay since
        //       (i) the command time would be difficult to define
        //      (ii) sources depending on state would operate on incomplete state
        //   2. we don't poll inputs when there are commands to poll since
        //       (i) commands should be processed as fast as possible
        //      (ii) command time more accurately reflects real time, even if app latency is now reflected as 'transfer' time
        //     (iii) input back pressure is better than falling behind and the problem becomes visible to sender
        return outputStep.perform() | sequencerStep.perform();
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
        return Loop.start(idleStrategy, exceptionHandler, threadFactory, this);
    }
}