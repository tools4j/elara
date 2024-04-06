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
package org.tools4j.elara.input;

import org.tools4j.elara.source.CommandContext;
import org.tools4j.elara.source.CommandSource;
import org.tools4j.elara.step.AgentStep;

import java.util.function.Function;

import static java.util.Objects.requireNonNull;

@FunctionalInterface
public interface Input {

    AgentStep inputPollerStep(CommandContext commandContext);

    Input NOOP = sourceContextProvider -> AgentStep.NOOP;

    static Input single(final SingleSourceInput input) {
        return single(input.sourceId(), input);
    }

    static Input single(final int sourceId, final InputPoller inputPoller) {
        requireNonNull(inputPoller);
        return commandContext -> {
            final CommandSource commandSource = commandContext.commandSources().sourceById(sourceId);
            return () -> inputPoller.poll(commandContext, commandSource.commandSender());
        };
    }

    static Input single(final int sourceId, final long initialSourceSequence, final InputPoller inputPoller) {
        requireNonNull(inputPoller);
        return commandContext -> {
            final CommandSource commandSource = commandContext.commandSources().sourceById(sourceId);
            commandSource.transientCommandState().sourceSequenceGenerator().nextSequence(initialSourceSequence);
            return () -> inputPoller.poll(commandContext, commandSource.commandSender());
        };
    }

    static Input multi(final MultiSourceInput input) {
        requireNonNull(input);
        return commandContext -> () -> input.poll(commandContext, commandContext.commandSources());
    }

    static Input composite(final Input... inputs) {
        return aggregate(AgentStep::composite, inputs);
    }

    static Input roundRobin(final Input... inputs) {
        return aggregate(AgentStep::roundRobin, inputs);
    }

    static Input aggregate(final Function<? super AgentStep[], ? extends AgentStep> aggregator,
                           final Input... inputs) {
        requireNonNull(aggregator);
        requireNonNull(inputs);
        return sourceContextProvider -> {
            final AgentStep[] steps = new AgentStep[inputs.length];
            for (int i = 0; i < steps.length; i++) {
                steps[i] = inputs[i].inputPollerStep(sourceContextProvider);
            }
            return aggregator.apply(steps);
        };
    }
}
