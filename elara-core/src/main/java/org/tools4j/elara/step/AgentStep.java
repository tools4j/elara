/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.concurrent.Agent;

import static java.util.Objects.requireNonNull;

/**
 * A step or part of an agent's {@link Agent#doWork()} method.
 */
@FunctionalInterface
public interface AgentStep {
    /**
     * An agent step should implement this method to do its work.
     * <p>
     * The return value is used for implementing a backoff strategy that can be employed when no work is
     * currently available for the agent to process.
     *
     * @return 0 to indicate no work was currently available, a positive value otherwise.
     */
    int doWork();

    /** Do-nothing step */
    AgentStep NOOP = () -> 0;

    static AgentStep composite(final AgentStep... steps) {
        requireNonNull(steps);
        return () -> {
            int workDone = 0;
            for (final AgentStep step : steps) {
                workDone += step.doWork();
            }
            return workDone;
        };
    }
}
