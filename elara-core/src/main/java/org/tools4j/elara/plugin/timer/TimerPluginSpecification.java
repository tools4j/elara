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
package org.tools4j.elara.plugin.timer;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.StateFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.PluginStateProvider;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin.SystemPluginSpecification;

import static java.util.Objects.requireNonNull;

final class TimerPluginSpecification implements SystemPluginSpecification<MutableTimerState> {
    private final TimerPlugin timerPlugin;

    public TimerPluginSpecification(final TimerPlugin timerPlugin) {
        this.timerPlugin = requireNonNull(timerPlugin);
    }

    @Override
    public PluginStateProvider<MutableTimerState> defaultPluginStateProvider() {
        return appConfig -> new SimpleTimerState();
    }

    @Override
    public ReservedPayloadType reservedPayloadType() {
        return ReservedPayloadType.TIMER;
    }

    @Override
    public Installer installer(final AppConfig appConfig, final MutableTimerState timerState) {
        requireNonNull(appConfig);
        requireNonNull(timerState);
        final TimerIdGenerator timerIdGenerator = new TimerIdGenerator();
        timerPlugin.init(appConfig.timeSource(), timerIdGenerator, timerState);
        return new Installer.Default() {
            @Override
            public Input input(final BaseState baseState) {
                final long sourceSeq = 1 + baseState.lastAppliedCommandSequence(timerPlugin.sourceId());
                return Input.single(timerPlugin.sourceId(), sourceSeq,
                        new TimerSignalPoller(appConfig.timeSource(), timerState,
                        timerPlugin.signalInputSkip()));
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new TimerCommandProcessor(timerState);
            }

            @Override
            public EventApplier eventApplier(final MutableBaseState baseState) {
                return new TimerEventApplier(timerPlugin, timerState, timerIdGenerator);
            }

            @Override
            public Interceptor interceptor(final StateFactory stateFactory) {
                return timerPlugin.useInterceptor() ? new TimerPluginInterceptor(timerPlugin) : Interceptor.NOOP;
            }
        };
    }
}
