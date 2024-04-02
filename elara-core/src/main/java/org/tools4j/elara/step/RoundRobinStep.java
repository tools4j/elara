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
package org.tools4j.elara.step;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.step.AgentStepMerger.merge;

/**
 * An agent step that delegates to an array of steps invoking them in a round-robin fashion.  At each invocation the
 * round-robin step iterates at most all delegate steps once and only until one step does some work.
 */
public final class RoundRobinStep implements AgentStep {

    private final AgentStep[] steps;

    private int roundRobinIndex = 0;

    public RoundRobinStep(final AgentStep... steps) {
        this.steps = requireNonNull(steps);
    }

    public static AgentStep simplify(final AgentStep... steps) {
        return new RoundRobinStep(merge(step -> step instanceof RoundRobinStep ? ((RoundRobinStep)step).steps : null,
                steps));
    }

    @Override
    public int doWork() {
        final int count = steps.length;
        for (int i = 0; i < count; i++) {
            final int index = getAndIncrementRoundRobinIndex(count);
            final int work = steps[index].doWork();
            if (work > 0) {
                return work;
            }
        }
        return 0;
    }

    private int getAndIncrementRoundRobinIndex(final int count) {
        final int index = roundRobinIndex;
        roundRobinIndex++;
        if (roundRobinIndex >= count) {
            roundRobinIndex = 0;
        }
        return index;
    }
}
