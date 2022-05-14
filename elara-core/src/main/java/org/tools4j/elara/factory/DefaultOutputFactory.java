/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.handler.DefaultOutputHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.CompositeOutput;
import org.tools4j.elara.output.DefaultCommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.PublisherStep;

import java.util.Arrays;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.step.PublisherStep.DEFAULT_POLLER_ID;

public class DefaultOutputFactory implements OutputFactory {

    private final Configuration configuration;
    private final Supplier<? extends Singletons> singletons;

    public DefaultOutputFactory(final Configuration configuration, final Supplier<? extends Singletons> singletons) {
        this.configuration = requireNonNull(configuration);
        this.singletons = requireNonNull(singletons);
    }

    @Override
    public Output output() {
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = singletons.get().plugins();
        if (plugins.length == 0) {
            return configuration.output();
        }
        final BaseState baseState = singletons.get().baseState();
        final Output[] outputs = new Output[plugins.length + 1];
        int count = 0;
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            outputs[count] = plugin.output(baseState);
            if (outputs[count] != Output.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return configuration.output();
        }
        outputs[count++] = configuration.output();//application output last
        return new CompositeOutput(
                count == outputs.length ? outputs : Arrays.copyOf(outputs, count),
                configuration.exceptionHandler()
        );
    }

    @Override
    public SequenceGenerator loopbackSequenceGenerator() {
        return new SimpleSequenceGenerator(configuration.timeSource().currentTime());
    }

    @Override
    public CommandLoopback commandLoopback() {
        return new DefaultCommandLoopback(
                configuration.commandStream().appender(),
                configuration.timeSource(),
                singletons.get().loopbackSequenceGenerator()
        );
    }

    @Override
    public OutputHandler outputHandler() {
        return new DefaultOutputHandler(
                singletons.get().output(), singletons.get().commandLoopback(), configuration.exceptionHandler()
        );
    }

    @Override
    public AgentStep publisherStep() {
        final OutputHandler outputHandler = singletons.get().outputHandler();
        try {
            return new PublisherStep(outputHandler, configuration.eventStore(), DEFAULT_POLLER_ID);
        } catch (final UnsupportedOperationException e) {
            //ignore, use non-tracking below
        }
        return new PublisherStep(outputHandler, configuration.eventStore());
    }

}
