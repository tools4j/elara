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
import org.tools4j.elara.app.state.EventProcessingState;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.step.AgentStep;

import static java.util.Objects.requireNonNull;

/**
 * Agent for running the elara tasks required for a feedback app where events are received from the sequencer.
 */
public class FeedbackAgent implements Agent {

    private final EventProcessingState eventProcessingState;
    private final CommandContext commandContext;
    private final AgentStep inputStep;
    private final AgentStep eventPollerStep;//poll + process + publish
    private final AgentStep extraStepAlways;
    private final AgentStep extraStepAlwaysWhenEventsApplied;
    private final boolean noInputsAndExtraStepWhenEventsApplied;

    public FeedbackAgent(final EventProcessingState eventProcessingState,
                         final CommandContext commandContext,
                         final AgentStep inputStep,
                         final AgentStep eventPollerStep,
                         final AgentStep extraStepAlwaysWhenEventsApplied,
                         final AgentStep extraStepAlways) {
        this.commandContext = requireNonNull(commandContext);
        this.eventProcessingState = requireNonNull(eventProcessingState);
        this.inputStep = requireNonNull(inputStep);
        this.eventPollerStep = requireNonNull(eventPollerStep);
        this.extraStepAlways = requireNonNull(extraStepAlways);
        this.extraStepAlwaysWhenEventsApplied = requireNonNull(extraStepAlwaysWhenEventsApplied);
        this.noInputsAndExtraStepWhenEventsApplied =
                inputStep == AgentStep.NOOP && extraStepAlwaysWhenEventsApplied == AgentStep.NOOP;
    }

    @Override
    public String roleName() {
        return "elara-feed";
    }

    @Override
    public int doWork() {
        int workDone;
        workDone = eventPollerStep.doWork();
        workDone += extraStepAlways.doWork();
        if (noInputsAndExtraStepWhenEventsApplied) {
            return workDone;
        }
        if (extraStepAlwaysWhenEventsApplied != AgentStep.NOOP && !commandContext.hasInFlightCommand()) {
            workDone += extraStepAlwaysWhenEventsApplied.doWork();
        }
        workDone += inputStep.doWork();
        return workDone;
    }
}
