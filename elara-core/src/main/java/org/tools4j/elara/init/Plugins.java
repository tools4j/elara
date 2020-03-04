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
package org.tools4j.elara.init;

import org.tools4j.elara.application.Application;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.command.CompositeCommandProcessor;
import org.tools4j.elara.event.CompositeEventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.plugin.Plugin;
import org.tools4j.elara.time.TimeSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Plugins {
    private static final Input[] EMPTY_INPUTS = new Input[0];

    <A extends Application> Plugins(final Context<A> context) {
        final A application = context.application();
        this.timeSource = context.timeSource();
        this.adminSequenceGenerator = new SimpleSequenceGenerator();
        this.plugins = plugins(application, context.plugins());
        this.inputs = inputs(context.inputs(), timeSource, adminSequenceGenerator, plugins);
        this.commandProcessor = commandProcessor(application, plugins);
        this.eventApplier = eventApplier(application, plugins);
    }

    final Plugin.Context[] plugins;
    final Input[] inputs;
    final TimeSource timeSource;
    final SequenceGenerator adminSequenceGenerator;
    final CommandProcessor commandProcessor;
    final EventApplier eventApplier;

    private static <A extends Application> Plugin.Context[] plugins(final A application,
                                                                    final List<Plugin.Builder<? super A>> builders) {
        final Plugin.Context[] plugins = new Plugin.Context[builders.size()];
        for (int i = 0; i < plugins.length; i++) {
            plugins[i] = builders.get(i).create(application);
        }
        return plugins;
    }

    private static CommandProcessor commandProcessor(final Application application,
                                                     final Plugin.Context... plugins) {
        if (plugins.length == 0) {
            return application.commandProcessor();
        }
        final CommandProcessor[] processors = new CommandProcessor[plugins.length + 1];
        int count = 1;
        for (final Plugin.Context plugin : plugins) {
            processors[count] = plugin.commandProcessor();
            if (processors[count] != CommandProcessor.NOOP) {
                count++;
            }
        }
        if (count == 1) {
            return application.commandProcessor();
        }
        processors[0] = application.commandProcessor();//application processor first
        return new CompositeCommandProcessor(
                count == processors.length ? processors : Arrays.copyOf(processors, count)
        );
    }

    private static EventApplier eventApplier(final Application application,
                                             final Plugin.Context... plugins) {
        if (plugins.length == 0) {
            return application.eventApplier();
        }
        final EventApplier[] appliers = new EventApplier[plugins.length + 1];
        int count = 0;
        for (final Plugin.Context plugin : plugins) {
            appliers[count] = plugin.eventApplier();
            if (appliers[count] != EventApplier.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return application.eventApplier();
        }
        appliers[count++] = application.eventApplier();//application applier last
        return new CompositeEventApplier(
                count == appliers.length ? appliers : Arrays.copyOf(appliers, count)
        );
    }

    private static Input[] inputs(final List<Input> inputs,
                                  final TimeSource timeSource,
                                  final SequenceGenerator adminSequenceGenerator,
                                  final Plugin.Context... plugins) {
        if (plugins.length == 0) {
            return inputs.toArray(EMPTY_INPUTS);
        }
        final List<Input> allInputs = new ArrayList<>(inputs.size() + 3 * plugins.length);
        allInputs.addAll(inputs);
        for (final Plugin.Context plugin : plugins) {
            allInputs.addAll(Arrays.asList(plugin.inputs(timeSource, adminSequenceGenerator)));
        }
        return allInputs.toArray(EMPTY_INPUTS);
    }
}
