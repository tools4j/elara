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
package org.tools4j.elara.agent;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.step.AgentStep;

import static java.util.Objects.requireNonNull;

/**
 * Agent for running the elara feedback tasks.  Feedback tasks are polling events and processing events.
 * Processing events consists of updating the state and sending commands back to the command stream.
 */
public class FeedbackAgent implements Agent {

    private final AgentStep eventStep;
    private final AgentStep inputStep;
    private final AgentStep publisherStep;
    private final AgentStep extraStepAlwaysWhenEventsApplied;
    private final AgentStep extraStepAlways;

    public FeedbackAgent(final AgentStep eventStep,
                         final AgentStep inputStep,
                         final AgentStep publisherStep,
                         final AgentStep extraStepAlwaysWhenEventsApplied,
                         final AgentStep extraStepAlways) {
        this.eventStep = requireNonNull(eventStep);
        this.inputStep = requireNonNull(inputStep);
        this.publisherStep = requireNonNull(publisherStep);
        this.extraStepAlwaysWhenEventsApplied = requireNonNull(extraStepAlwaysWhenEventsApplied);
        this.extraStepAlways = requireNonNull(extraStepAlways);
    }

    @Override
    public String roleName() {
        return "elara-feed";
    }

    @Override
    public int doWork() {
        int workDone;
        if ((workDone = eventStep.doWork()) > 0) {
            workDone += publisherStep.doWork();
            workDone += extraStepAlways.doWork();
            return workDone;
        }
        if ((workDone = inputStep.doWork()) > 0) {
            workDone += publisherStep.doWork();
            workDone += extraStepAlwaysWhenEventsApplied.doWork();
            workDone += extraStepAlways.doWork();
            return workDone;
        }
    }
}
