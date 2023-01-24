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
import org.tools4j.elara.step.SequencerStep;

import static java.util.Objects.requireNonNull;

/**
 * Agent for running {@link SequencerStep} and {@link ProcessorAgent processor agent} steps.
 */
public class CoreAgent implements Agent {

    private final AgentStep sequencerStep;
    private final AgentStep commandStep;
    private final AgentStep eventStep;
    private final AgentStep extraStepAlways;
    private final AgentStep extraStepAlwaysWhenEventsApplied;

    public CoreAgent(final AgentStep sequencerStep,
                     final AgentStep commandStep,
                     final AgentStep eventStep,
                     final AgentStep extraStepAlwaysWhenEventsApplied,
                     final AgentStep extraStepAlways) {
        this.sequencerStep = requireNonNull(sequencerStep);
        this.commandStep = requireNonNull(commandStep);
        this.eventStep = requireNonNull(eventStep);
        this.extraStepAlways = requireNonNull(extraStepAlways);
        this.extraStepAlwaysWhenEventsApplied = requireNonNull(extraStepAlwaysWhenEventsApplied);
    }

    @Override
    public String roleName() {
        return "elara-core";
    }

    @Override
    public int doWork() {
        int workDone;
        if ((workDone = eventStep.doWork()) > 0) {
            workDone += extraStepAlways.doWork();
            return workDone;
        }
        if ((workDone = commandStep.doWork()) > 0) {
            workDone += extraStepAlwaysWhenEventsApplied.doWork();
            workDone += extraStepAlways.doWork();
            return workDone;
        }
        //NOTES:
        //   1. we don't poll inputs during replay since
        //       (i) the command time would be difficult to define
        //      (ii) sources depending on state would operate on incomplete state
        //   2. we don't poll inputs when there are commands to poll since
        //       (i) commands should be processed as fast as possible
        //      (ii) command time more accurately reflects real time, even if app latency is now reflected as 'transfer' time
        //     (iii) we prefer input back pressure over falling behind as it makes the problem visible and signals it to senders
        return sequencerStep.doWork() + extraStepAlwaysWhenEventsApplied.doWork() + extraStepAlways.doWork();
    }
}
