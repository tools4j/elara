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
package org.tools4j.elara.agent;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.step.AgentStep;

import static java.util.Objects.requireNonNull;

/**
 * Agent for running the elara processor tasks.  Processor tasks are polling (or replaying) and applying events as well
 * as and processing commands.
 */
public class ProcessorAgent implements Agent {

    private final AgentStep commandStep;
    private final AgentStep eventStep;
    private final AgentStep extraStepAlwaysWhenEventsApplied;

    public ProcessorAgent(final AgentStep commandStep,
                          final AgentStep eventStep,
                          final AgentStep extraStepAlwaysWhenEventsApplied) {
        this.commandStep = requireNonNull(commandStep);
        this.eventStep = requireNonNull(eventStep);
        this.extraStepAlwaysWhenEventsApplied = requireNonNull(extraStepAlwaysWhenEventsApplied);
    }

    @Override
    public String roleName() {
        return "elara-core";
    }

    @Override
    public int doWork() {
        final int workDone = eventStep.doWork();
        if (workDone > 0) {
            return workDone;
        }
        return commandStep.doWork() + extraStepAlwaysWhenEventsApplied.doWork();
    }
}
