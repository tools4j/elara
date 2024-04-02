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
package org.tools4j.elara.app.factory;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ExecutionType;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;
import org.tools4j.elara.step.AgentStep;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.app.config.ExecutionType.ALWAYS;
import static org.tools4j.elara.app.config.ExecutionType.ALWAYS_WHEN_EVENTS_APPLIED;
import static org.tools4j.elara.step.AgentStep.NOOP;

public class DefaultAgentStepFactory implements AgentStepFactory {

    private final AppConfig config;
    private final BaseState baseState;
    private final Installer[] plugins;

    public DefaultAgentStepFactory(final AppConfig config,
                                   final BaseState baseState,
                                   final Installer[] plugins) {
        this.config = requireNonNull(config);
        this.baseState = requireNonNull(baseState);
        this.plugins = requireNonNull(plugins);
    }

    @Override
    public AgentStep extraStepAlwaysWhenEventsApplied() {
        return extraStep(ALWAYS_WHEN_EVENTS_APPLIED);
    }

    @Override
    public AgentStep extraStepAlways() {
        return extraStep(ALWAYS);
    }

    private AgentStep extraStep(final ExecutionType executionType) {
        final List<AgentStep> dutyCycleExtraSteps = config.dutyCycleExtraSteps(executionType);
        if (plugins.length == 0 && dutyCycleExtraSteps.isEmpty()) {
            return NOOP;
        }
        final List<AgentStep> extraSteps = new ArrayList<>(plugins.length + dutyCycleExtraSteps.size());
        for (final Installer plugin : plugins) {
            extraSteps.add(plugin.step(baseState, executionType));
        }
        for (int i = 0; i < dutyCycleExtraSteps.size(); i++) {
            extraSteps.add(dutyCycleExtraSteps.get(i));
        }
        extraSteps.removeIf(step -> step == NOOP);
        if (extraSteps.isEmpty()) {
            return NOOP;
        }
        if (extraSteps.size() == 1) {
            return extraSteps.get(0);
        }
        return AgentStep.composite(extraSteps.toArray(new AgentStep[0]));
    }
}
