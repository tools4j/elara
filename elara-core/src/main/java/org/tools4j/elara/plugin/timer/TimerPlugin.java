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
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;

/**
 * Simple timer plugin to support timers using {@link TimerCommands} and {@link TimerEvents}.
 */
public final class TimerPlugin implements Plugin<TimerState.Mutable> {

    @Override
    public <A> Builder<A> builder() {
        return application -> create(new SimpleTimerState());
    }

    @Override
    public Context create(final TimerState.Mutable timerState) {
        requireNonNull(timerState);
        return new Context() {
            @Override
            public Input[] inputs(final BaseState baseState, final TimeSource timeSource, final SequenceGenerator adminSequenceGenerator) {
                return new Input[] {
                        new TimerTriggerInput(timerState, timeSource, adminSequenceGenerator)
                };
            }

            @Override
            public Output output(final BaseState baseState) {
                return new TimerCommandLoopback(timerState);
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
