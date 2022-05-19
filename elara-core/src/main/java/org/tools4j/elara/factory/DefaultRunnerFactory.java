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
package org.tools4j.elara.factory;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.agent.CoreAgent;
import org.tools4j.elara.app.config.Configuration;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.step.AgentStep;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.app.config.ExecutionType.ALWAYS;
import static org.tools4j.elara.app.config.ExecutionType.ALWAYS_WHEN_EVENTS_APPLIED;
import static org.tools4j.elara.app.config.ExecutionType.INIT_ONCE_ONLY;
import static org.tools4j.elara.step.AgentStep.NO_OP;

public class DefaultRunnerFactory implements RunnerFactory {

    private final Configuration configuration;
    private final Supplier<? extends Singletons> singletons;

    public DefaultRunnerFactory(final Configuration configuration, final Supplier<? extends Singletons> singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public Runnable initStep() {
        return extraStep(INIT_ONCE_ONLY)::doWork;
    }

    @Override
    public AgentStep extraStepAlwaysWhenEventsApplied() {
        return extraStep(ALWAYS_WHEN_EVENTS_APPLIED);
    }

    @Override
    public AgentStep extraStepAlways() {
        return extraStep(ALWAYS);
    }

    @Override
    public Agent elaraAgent() {
        final Singletons factory = singletons.get();
        return new CoreAgent(factory.sequencerStep(), factory.commandPollerStep(), factory.eventPollerStep(),
                factory.extraStepAlwaysWhenEventsApplied(), factory.extraStepAlways());
//        return new AllInOneAgent(factory.sequencerStep(), factory.commandPollerStep(), factory.eventPollerStep(),
//                factory.publisherStep(), factory.extraStepAlwaysWhenEventsApplied(), factory.extraStepAlways());
    }

    private AgentStep extraStep(final ExecutionType executionType) {
        final Plugin.Configuration[] plugins = singletons.get().plugins();
        final List<AgentStep> dutyCycleExtraSteps = configuration.dutyCycleExtraSteps(executionType);
        if (plugins.length == 0 && dutyCycleExtraSteps.isEmpty()) {
            return NO_OP;
        }
        final List<AgentStep> extraSteps = new ArrayList<>(plugins.length + dutyCycleExtraSteps.size());
        final BaseState baseState = singletons.get().baseState();
        for (final Plugin.Configuration plugin : plugins) {
            extraSteps.add(plugin.step(baseState, executionType));
        }
        for (int i = 0; i < dutyCycleExtraSteps.size(); i++) {
            extraSteps.add(dutyCycleExtraSteps.get(i));
        }
        extraSteps.removeIf(step -> step == NO_OP);
        if (extraSteps.isEmpty()) {
            return NO_OP;
        }
        if (extraSteps.size() == 1) {
            return extraSteps.get(0);
        }
        return AgentStep.composite(extraSteps.toArray(new AgentStep[0]));
    }
}
