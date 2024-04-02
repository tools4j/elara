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
package org.tools4j.elara.agent;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.step.AgentStep;

import static java.util.Objects.requireNonNull;

/**
 * Agent for running the elara tasks required for a passthrough app where commands are directly routed as events with
 * the same payload as the command.
 */
public class PassthroughAgent implements Agent {

    private final AgentStep sequencerStep;
    private final AgentStep eventStep;
    private final AgentStep publisherStep;
    private final AgentStep extraStepAlways;
    private final AgentStep extraStepAlwaysWhenEventsApplied;

    public PassthroughAgent(final AgentStep sequencerStep,
                            final AgentStep eventStep,
                            final AgentStep publisherStep,
                            final AgentStep extraStepAlwaysWhenEventsApplied,
                            final AgentStep extraStepAlways) {
        this.sequencerStep = requireNonNull(sequencerStep);
        this.eventStep = requireNonNull(eventStep);
        this.publisherStep = requireNonNull(publisherStep);
        this.extraStepAlways = requireNonNull(extraStepAlways);
        this.extraStepAlwaysWhenEventsApplied = requireNonNull(extraStepAlwaysWhenEventsApplied);
    }

    @Override
    public String roleName() {
        return "elara-pass";
    }

    @Override
    public int doWork() {
        int workDone;
        if ((workDone = eventStep.doWork()) > 0) {
            workDone += publisherStep.doWork();
            workDone += extraStepAlways.doWork();
            return workDone;
        }
        return publisherStep.doWork() + sequencerStep.doWork() + extraStepAlwaysWhenEventsApplied.doWork() + extraStepAlways.doWork();
    }
}
