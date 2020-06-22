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
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.api.TypeRange;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.timer.TimerState.Mutable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Simple timer plugin to support timers using {@link TimerCommands} and {@link TimerEvents}.
 */
public class TimerPlugin implements SystemPlugin<TimerState.Mutable> {

    public static final int DEFAULT_COMMAND_SOURCE = -10;
    public static final TimerPlugin DEFAULT = new TimerPlugin(DEFAULT_COMMAND_SOURCE);

    private final int commandSource;

    public TimerPlugin(final int commandSource) {
        this.commandSource = commandSource;
    }

    @Override
    public TypeRange typeRange() {
        return TypeRange.TIMER;
    }

    @Override
    public Mutable defaultPluginState() {
        return new SimpleTimerState();
    }

    @Override
    public Configuration configuration(final org.tools4j.elara.init.Configuration appConfig, final Mutable timerState) {
        requireNonNull(appConfig);
        requireNonNull(timerState);
        return new Configuration.Default() {
            final TimerTrigger timerTrigger = new TimerTrigger(timerState);

            @Override
            public Input[] inputs(final BaseState baseState) {
                return new Input[] {
                        timerTrigger.asInput(commandSource, appConfig.timeSource())
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TimerPlugin that = (TimerPlugin) o;
        return commandSource == that.commandSource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(commandSource);
    }
}
