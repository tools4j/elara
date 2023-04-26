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
package org.tools4j.elara.app.factory;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.ApplierConfig;
import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.MutableBaseState;
import org.tools4j.elara.app.state.SingleEventBaseState;
import org.tools4j.elara.event.CompositeEventApplier;
import org.tools4j.elara.handler.DefaultEventHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.EventReplayStep;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultApplierFactory implements ApplierFactory {
    private final AppConfig appConfig;
    private final ApplierConfig applierConfig;
    private final EventStoreConfig eventStoreConfig;
    private final MutableBaseState baseState;
    private final Plugin.Configuration[] plugins;
    private final Supplier<? extends ApplierFactory> applierSingletons;

    public DefaultApplierFactory(final AppConfig appConfig,
                                 final ApplierConfig applierConfig,
                                 final EventStoreConfig eventStoreConfig,
                                 final MutableBaseState baseState,
                                 final Plugin.Configuration[] plugins,
                                 final Supplier<? extends ApplierFactory> applierSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.applierConfig = requireNonNull(applierConfig);
        this.eventStoreConfig = requireNonNull(eventStoreConfig);
        this.baseState = requireNonNull(baseState);
        this.plugins = requireNonNull(plugins);
        this.applierSingletons = requireNonNull(applierSingletons);
    }

    @Override
    public EventApplier eventApplier() {
        final EventApplier eventApplier = applierConfig.eventApplier();
        if (plugins.length == 0) {
            return eventApplier;
        }
        final EventApplier[] appliers = new EventApplier[plugins.length + 1];
        int count = 0;
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            appliers[count] = plugin.eventApplier(baseState);
            if (appliers[count] != EventApplier.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return eventApplier;
        }
        appliers[count++] = eventApplier;//application applier last
        return new CompositeEventApplier(
                count == appliers.length ? appliers : Arrays.copyOf(appliers, count)
        );
    }

    @Override
    public EventHandler eventHandler() {
        return new DefaultEventHandler(
                baseState,
                applierSingletons.get().eventApplier(),
                appConfig.exceptionHandler(),
                eventStoreConfig.duplicateHandler()
        );
    }

    @Override
    public AgentStep eventPollerStep() {
        final EventHandler eventHandler = applierSingletons.get().eventHandler();
        if (baseState instanceof SingleEventBaseState) {
            return EventReplayStep.replayAllEvents(eventStoreConfig.eventStore(), eventHandler);
        }
        return EventReplayStep.replayNonAbortedEvents(eventStoreConfig.eventStore(), eventHandler);
    }
}
