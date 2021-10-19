/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.loop;

import org.tools4j.nobark.loop.Step;

import static java.util.Objects.requireNonNull;

public class DutyCycleStep implements Step {

    private final Step sequencerStep;
    private final Step commandPollerStep;
    private final Step eventPollerStep;
    private final Step outputStep;
    private final Step extraStep;

    public DutyCycleStep(final Step sequencerStep,
                         final Step commandPollerStep,
                         final Step eventPollerStep,
                         final Step outputStep,
                         final Step extraStep) {
        this.sequencerStep = requireNonNull(sequencerStep);
        this.commandPollerStep = requireNonNull(commandPollerStep);
        this.eventPollerStep = requireNonNull(eventPollerStep);
        this.outputStep = requireNonNull(outputStep);
        this.extraStep = requireNonNull(extraStep);
    }


    @Override
    public boolean perform() {
        if (eventPollerStep.perform()) {
            outputStep.perform();
            return true;
        }
        if (commandPollerStep.perform()) {
            outputStep.perform();
            extraStep.perform();
            return true;
        }
        //NOTES:
        //   1. we don't poll inputs during replay since
        //       (i) the command time would be difficult to define
        //      (ii) sources depending on state would operate on incomplete state
        //   2. we don't poll inputs when there are commands to poll since
        //       (i) commands should be processed as fast as possible
        //      (ii) command time more accurately reflects real time, even if app latency is now reflected as 'transfer' time
        //     (iii) we prefer input back pressure over falling behind as it makes the problem visible and signals it to senders
        return outputStep.perform() | extraStep.perform() | sequencerStep.perform();
    }
}
