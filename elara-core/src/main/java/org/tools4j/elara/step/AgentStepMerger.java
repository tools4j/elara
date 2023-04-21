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

import java.util.function.Function;

enum AgentStepMerger {
    ;

    static boolean isNoOp(final AgentStep step) {
        return step == null || step == AgentStep.NOOP;
    }

    static AgentStep[] merge(final Function<AgentStep, AgentStep[]> exploder,
                             final AgentStep... steps) {
        int increment = 0;
        for (final AgentStep step : steps) {
            final AgentStep[] exploded = exploder.apply(step);
            increment += exploded == null ? isNoOp(step) ? -1 : 0 : exploded.length - 1;
        }
        if (increment == 0) {
            return steps;
        }
        final AgentStep[] merged = new AgentStep[steps.length + increment];
        int index = 0;
        for (final AgentStep step : steps) {
            final AgentStep[] exploded = exploder.apply(step);
            if (exploded != null) {
                for (final AgentStep explodedStep : exploded) {
                    merged[index] = explodedStep;
                    index++;
                }
            } else if (!isNoOp(step)) {
                merged[index] = step;
                index++;
            }
        }
        assert index == merged.length;
        return merged;
    }
}
