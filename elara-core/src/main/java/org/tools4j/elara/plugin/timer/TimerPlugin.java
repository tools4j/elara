/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.timer.TimerState.Mutable;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Simple timer plugin to support timers using {@link TimerCommands} and {@link TimerEvents}.
 */
public class TimerPlugin implements SystemPlugin<TimerState.Mutable> {

    public static final int DEFAULT_SOURCE_ID = -10;
    public static final TimerPlugin DEFAULT = new TimerPlugin(DEFAULT_SOURCE_ID);

    private final int sourceId;

    public TimerPlugin(final int sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public ReservedPayloadType reservedPayloadType() {
        return ReservedPayloadType.TIMER;
    }

    @Override
    public Mutable defaultPluginState(final AppConfig appConfig) {
        return new SimpleTimerState();
    }

    @Override
    public Configuration configuration(final AppConfig appConfig, final Mutable timerState) {
        requireNonNull(appConfig);
        requireNonNull(timerState);
        return new Configuration.Default() {
            TimerTriggerInput timerTriggerInput;

            SequenceGenerator sourceSeqGenerator(final BaseState baseState) {
                return new SimpleSequenceGenerator(1 + baseState.lastAppliedCommandSequence(sourceId));
            }

            TimerTriggerInput timerTriggerInput(final BaseState baseState) {
                if (timerTriggerInput == null) {
                    timerTriggerInput = new TimerTriggerInput(sourceId, sourceSeqGenerator(baseState),
                            appConfig.timeSource(), timerState);
                }
                return timerTriggerInput;
            }

            @Override
            public Input[] inputs(final BaseState baseState) {
                return new Input[]{timerTriggerInput(baseState)};
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new TimerCommandProcessor(timerState);
            }

            @Override
            public EventApplier eventApplier(final BaseState.Mutable baseState) {
                return new TimerEventApplier(timerState, timerTriggerInput(baseState));
            }
        };
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TimerPlugin that = (TimerPlugin) o;
        return sourceId == that.sourceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceId);
    }
}
