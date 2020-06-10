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

import org.tools4j.elara.init.Configuration;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.ReceiverFactory;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.loop.SequencerStep;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.time.TimeSource;
import org.tools4j.nobark.loop.Step;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.input.Input.EMPTY_INPUTS;

public class DefaultInputFactory implements InputFactory {

    private final ElaraFactory elaraFactory;

    public DefaultInputFactory(final ElaraFactory elaraFactory) {
        this.elaraFactory = requireNonNull(elaraFactory);
    }

    protected ElaraFactory elaraFactory() {
        return elaraFactory;
    }

    protected Configuration configuration() {
        return elaraFactory.configuration();
    }

    @Override
    public TimeSource timeSource() {
        return configuration().timeSource();
    }

    @Override
    public Input[] inputs() {
        final List<Input> inputs = configuration().inputs();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = elaraFactory().pluginFactory().plugins();
        if (plugins.length == 0) {
            return inputs.toArray(EMPTY_INPUTS);
        }
        final BaseState baseState = elaraFactory().pluginFactory().baseState();
        final TimeSource timeSource = timeSource();
        final List<Input> allInputs = new ArrayList<>(inputs.size() + 3 * plugins.length);
        allInputs.addAll(inputs);
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            allInputs.addAll(Arrays.asList(plugin.inputs(baseState)));
        }
        return allInputs.toArray(EMPTY_INPUTS);
    }

    @Override
    public ReceiverFactory receiverFactory() {
        final MessageLog.Appender commandAppender = configuration().commandLog().appender();
        return new ReceiverFactory(configuration().timeSource(), commandAppender);
    }

    @Override
    public Step sequencerStep() {
        return new SequencerStep(receiverFactory(), elaraFactory().inputFactory().inputs());
    }

}
