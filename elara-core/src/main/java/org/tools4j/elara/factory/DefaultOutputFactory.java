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
package org.tools4j.elara.factory;

import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.handler.DefaultOutputHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.loop.OutputStep;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.CompositeOutput;
import org.tools4j.elara.output.DefaultCommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.nobark.loop.Step;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.loop.OutputStep.DEFAULT_POLLER_ID;

public class DefaultOutputFactory implements OutputFactory {

    private final ElaraFactory elaraFactory;

    public DefaultOutputFactory(final ElaraFactory elaraFactory) {
        this.elaraFactory = requireNonNull(elaraFactory);
    }

    protected ElaraFactory elaraFactory() {
        return elaraFactory;
    }

    protected Context context() {
        return elaraFactory.context();
    }

    @Override
    public Output output() {
        final Plugin.Context[] plugins = elaraFactory().pluginFactory().plugins();
        if (plugins.length == 0) {
            return context().output();
        }
        final BaseState baseState = elaraFactory().pluginFactory().baseState();
        final Output[] outputs = new Output[plugins.length + 1];
        int count = 0;
        for (final Plugin.Context plugin : plugins) {
            outputs[count] = plugin.output(baseState);
            if (outputs[count] != EventApplier.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return context().output();
        }
        outputs[count++] = context().output();//application output last
        return new CompositeOutput(
                count == outputs.length ? outputs : Arrays.copyOf(outputs, count)
        );
    }

    @Override
    public SequenceGenerator loopbackSequenceGenerator() {
        return new SimpleSequenceGenerator();
    }

    @Override
    public CommandLoopback commandLoopback() {
        return new DefaultCommandLoopback(
                context().commandLog().appender(),
                context().timeSource(),
                loopbackSequenceGenerator()
        );
    }

    @Override
    public OutputHandler outputHandler() {
        return new DefaultOutputHandler(output(), commandLoopback(), context().exceptionHandler());
    }

    @Override
    public Step outputStep() {
        if (context().output() == Output.NOOP) {
            return Step.NO_OP;
        }
        final OutputHandler outputHandler = outputHandler();
        try {
            return new OutputStep(outputHandler, context().eventLog(), DEFAULT_POLLER_ID);
        } catch (final UnsupportedOperationException e) {
            //ignore, use non-tracking below
        }
        return new OutputStep(outputHandler, context().eventLog());
    }

}