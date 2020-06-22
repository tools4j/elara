/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.loop.DutyCycleStep;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.loop.Step;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.tools4j.nobark.loop.ComposableStep.composite;
import static org.tools4j.nobark.loop.Step.NO_OP;

public class DefaultRunnerFactory implements RunnerFactory {

    private final Configuration configuration;
    private final org.tools4j.elara.factory.Singletons singletons;

    public DefaultRunnerFactory(final Configuration configuration, final org.tools4j.elara.factory.Singletons singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public Runnable initStep() {
        return () -> {};
    }

    @Override
    public LoopCondition runningCondition() {
        return workDone -> true;
    }

    @Override
    public Step dutyCycleStep() {
        final List<Step> pluginSteps = pluginSteps(false);
        final List<Step> extraSteps = configuration.dutyCycleExtraSteps(false);
        final Step extraStep;
        if (pluginSteps.isEmpty() && extraSteps.isEmpty()) {
            extraStep = NO_OP;
        } else {
            final Step[] allExtraSteps = new Step[pluginSteps.size() + extraSteps.size()];
            for (int i = 0; i < pluginSteps.size(); i++) {
                allExtraSteps[i] = pluginSteps.get(i);
            }
            final int off = pluginSteps.size();
            for (int i = 0; i < extraSteps.size(); i++) {
                allExtraSteps[off + i] = extraSteps.get(i);
            }
            extraStep = composite(allExtraSteps);
        }
        return new DutyCycleStep(singletons.sequencerStep(), singletons.commandPollerStep(),
                singletons.eventApplierStep(), singletons.outputStep(), extraStep);
    }

    @Override
    public Step[] dutyCycleWithExtraSteps() {
        final List<Step> pluginStepsAlways = pluginSteps(true);
        final List<Step> extraStepsAlways = configuration.dutyCycleExtraSteps(true);
        final Step[] dutyCycle = new Step[1 + pluginStepsAlways.size() + extraStepsAlways.size()];
        dutyCycle[0] = singletons.dutyCycleStep();
        int offset = 1;
        for (int i = 0; i < pluginStepsAlways.size(); i++) {
            dutyCycle[offset + i] = pluginStepsAlways.get(i);
        }
        offset = 1 + pluginStepsAlways.size();
        for (int i = 0; i < extraStepsAlways.size(); i++) {
            dutyCycle[offset + i] = extraStepsAlways.get(i);
        }
        return dutyCycle;
    }

    private List<Step> pluginSteps(final boolean alwaysExecute) {
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = singletons.plugins();
        if (plugins.length == 0) {
            return Collections.emptyList();
        }
        final BaseState baseState = singletons.baseState();
        final List<Step> steps = new ArrayList<>(plugins.length);
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            steps.add(plugin.step(baseState, alwaysExecute));
        }
        return steps;
    }
}
