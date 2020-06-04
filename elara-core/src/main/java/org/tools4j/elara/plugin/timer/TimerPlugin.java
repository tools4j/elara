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
package org.tools4j.elara.plugin.timer;

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.timer.TimerState.Mutable;
import org.tools4j.elara.time.TimeSource;

/**
 * Simple timer plugin to support timers using {@link TimerCommands} and {@link TimerEvents}.
 */
public final class TimerPlugin implements Plugin<TimerState.Mutable> {

    public static final int INPUT_ID = -10;

    @Override
    public Mutable defaultPluginState() {
        return new SimpleTimerState();
    }

    @Override
    public Context context(final TimerState.Mutable timerState) {
        return new Context() {
            final TimerTrigger timerTrigger = new TimerTrigger(timerState);
            @Override
            public Input[] inputs(final BaseState baseState, final TimeSource timeSource) {
                return new Input[] {
                        timerTrigger.asInput(INPUT_ID, timeSource)
                };
            }

            @Override
            public Output output(final BaseState baseState) {
                return timerTrigger.asOutput();
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new TimerCommandProcessor(timerState);
            }

            @Override
            public EventApplier eventApplier(final BaseState.Mutable baseState) {
                return new TimerEventApplier(timerState);
            }
        };
    }

}
