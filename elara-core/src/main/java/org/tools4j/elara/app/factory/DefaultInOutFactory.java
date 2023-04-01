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
import org.tools4j.elara.app.config.InOutConfig;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.CompositeOutput;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.plugin.boot.BootCommandInput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.api.Plugin.NO_INPUTS;

public class DefaultInOutFactory implements InOutFactory {

    private final AppConfig appConfig;
    private final InOutConfig inOutConfig;
    private final Supplier<? extends PluginFactory> pluginSingletons;

    public DefaultInOutFactory(final AppConfig appConfig,
                               final InOutConfig inOutConfig,
                               final Supplier<? extends PluginFactory> pluginSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.inOutConfig = requireNonNull(inOutConfig);
        this.pluginSingletons = requireNonNull(pluginSingletons);
    }

    @Override
    public Input[] inputs() {
        final List<Input> inputs = inOutConfig.inputs();
        final org.tools4j.elara.plugin.api.Plugin.Configuration[] plugins = pluginSingletons.get().plugins();
        if (plugins.length == 0) {
            return inputs.toArray(NO_INPUTS);
        }
        final BaseState baseState = pluginSingletons.get().baseState();
        final List<Input> allInputs = new ArrayList<>(inputs.size() + 3 * plugins.length);
        allInputs.addAll(inputs);
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            allInputs.addAll(Arrays.asList(plugin.inputs(baseState)));
        }
        return moveBootInputToStart(allInputs.toArray(NO_INPUTS));
    }

    //NOTE: we want boot plugin inputs at the start so that those commands get polled first of all
    private static Input[] moveBootInputToStart(final Input[] inputs) {
        int bootInputs = 0;
        for (int i = inputs.length - 1; i >= 0; i--) {
            if (inputs[i] instanceof BootCommandInput) {
                bootInputs++;
            } else if (bootInputs > 0) {
                final Input nonBootInput = inputs[i];
                System.arraycopy(inputs, i + 1, inputs, i, bootInputs);
                inputs[i + bootInputs] = nonBootInput;
            }
        }
        return inputs;
    }

    @Override
    public Output output() {
        final Plugin.Configuration[] plugins = pluginSingletons.get().plugins();
        if (plugins.length == 0) {
            return inOutConfig.output();
        }
        final BaseState baseState = pluginSingletons.get().baseState();
        final Output[] outputs = new Output[plugins.length + 1];
        int count = 0;
        for (final org.tools4j.elara.plugin.api.Plugin.Configuration plugin : plugins) {
            outputs[count] = plugin.output(baseState);
            if (outputs[count] != Output.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return inOutConfig.output();
        }
        outputs[count++] = inOutConfig.output();//application output last
        return new CompositeOutput(
                count == outputs.length ? outputs : Arrays.copyOf(outputs, count),
                appConfig.exceptionHandler()
        );
    }
}
