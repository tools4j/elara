/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.factory.RunnerFactory;
import org.tools4j.nobark.loop.ExceptionHandler;
import org.tools4j.nobark.loop.IdleStrategy;
import org.tools4j.nobark.loop.Loop;
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.loop.Step;
import org.tools4j.nobark.run.StoppableThread;

import java.util.concurrent.ThreadFactory;

import static java.util.Objects.requireNonNull;

/**
 * Extension of nobark {@link Loop} allowing for an initialisation step executed in the elera thread before the loop.
 * The static {@link #start(IdleStrategy, ExceptionHandler, ThreadFactory, RunnerFactory) start(..)}
 * method takes the {@link RunnerFactory} and initialises most elara objects on the target thread.
 */
public class ElaraLoop extends Loop {

    private final Runnable initStep;

    /**
     * Constructor with loop condition, idle strategy, step exception handler and the steps to perform.
     *
     * @param loopCondition     the condition defining when the loop terminates
     * @param idleStrategy      the idle strategy defining how to handle situations without work to do
     * @param exceptionHandler  the handler for step exceptions
     * @param initStep          initialisation step run once before running the loop
     * @param steps             the steps executed in the loop
     */
    public ElaraLoop(final LoopCondition loopCondition,
                     final IdleStrategy idleStrategy,
                     final ExceptionHandler exceptionHandler,
                     final Runnable initStep,
                     final Step... steps) {
        super(loopCondition, idleStrategy, exceptionHandler, steps);
        this.initStep = requireNonNull(initStep);
    }

    /**
     * Creates, starts and returns a new thread running a loop with the given steps.
     *
     * @param idleStrategy      the strategy handling idle loop phases
     * @param exceptionHandler  the step exception handler
     * @param threadFactory     the factory to provide the service thread
     * @param runnerFactory     factory to create the objects to run on the elara thread
     * @return the newly created and started thread running the loop
     */
    public static StoppableThread start(final IdleStrategy idleStrategy,
                                        final ExceptionHandler exceptionHandler,
                                        final ThreadFactory threadFactory,
                                        final RunnerFactory runnerFactory) {
        requireNonNull(idleStrategy);
        requireNonNull(exceptionHandler);
        requireNonNull(threadFactory);
        requireNonNull(runnerFactory);
        return StoppableThread.start(running -> {
            final Runnable initStep = runnerFactory.initStep();
            final LoopCondition runCondition = runnerFactory.runningCondition();
            final Step[] dutyCycle = runnerFactory.dutyCycleWithExtraSteps();
            return new ElaraLoop(
                    workDone -> runCondition.loopAgain(workDone) && running.keepRunning(),
                    idleStrategy, exceptionHandler, initStep, dutyCycle);
        }, threadFactory);
    }

    @Override
    public void run() {
        initStep.run();
        super.run();
    }

}
