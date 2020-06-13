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
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.loop.Step;

import java.util.List;

import static java.util.Objects.requireNonNull;

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
        return new DutyCycleStep(singletons.sequencerStep(), singletons.commandPollerStep(),
                singletons.eventApplierStep(), singletons.outputStep());
    }

    @Override
    public Step[] dutyCycleWithExtraSteps() {
        final List<Step> extraSteps = configuration.dutyCycleExtraSteps();
        final Step[] dutyCycle = new Step[1 + extraSteps.size()];
        dutyCycle[0] = singletons.dutyCycleStep();
        for (int i = 1; i < dutyCycle.length; i++) {
            dutyCycle[i] = extraSteps.get(i - 1);
        }
        return dutyCycle;
    }
}
