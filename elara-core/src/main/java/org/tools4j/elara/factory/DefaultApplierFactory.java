/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.event.CompositeEventApplier;
import org.tools4j.elara.handler.DefaultEventHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.loop.EventPollerStep;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.nobark.loop.Step;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultApplierFactory implements ApplierFactory {

    private final Configuration configuration;
    private final Supplier<? extends Singletons> singletons;

    public DefaultApplierFactory(final Configuration configuration, final Supplier<? extends Singletons> singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public EventApplier eventApplier() {
        final EventApplier eventApplier = configuration.eventApplier();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = singletons.get().plugins();
        if (plugins.length == 0) {
            return eventApplier;
        }
        final BaseState.Mutable baseState = singletons.get().baseState();
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
                singletons.get().baseState(),
                singletons.get().eventApplier(),
                configuration.exceptionHandler(),
                configuration.duplicateHandler()
        );
    }

    @Override
    public Step eventPollerStep() {
        return new EventPollerStep(configuration.eventLog().poller(), singletons.get().eventHandler());
    }
}
