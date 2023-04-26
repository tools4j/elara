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
import org.tools4j.elara.app.config.OutputConfig;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.output.CompositeOutput;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;

import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class DefaultOutputFactory implements OutputFactory {

    private final AppConfig appConfig;
    private final OutputConfig outputConfig;
    private final BaseState baseState;
    private final Plugin.Configuration[] plugins;

    public DefaultOutputFactory(final AppConfig appConfig,
                                final OutputConfig outputConfig,
                                final BaseState baseState,
                                final Plugin.Configuration[] plugins) {
        this.appConfig = requireNonNull(appConfig);
        this.outputConfig = requireNonNull(outputConfig);
        this.baseState = requireNonNull(baseState);
        this.plugins = requireNonNull(plugins);
    }

    @Override
    public Output output() {
        if (plugins.length == 0) {
            return outputConfig.output();
        }
        final Output[] outputs = new Output[plugins.length + 1];
        int count = 0;
        for (final Plugin.Configuration plugin : plugins) {
            outputs[count] = plugin.output(baseState);
            if (outputs[count] != Output.NOOP) {
                count++;
            }
        }
        if (count == 0) {
            return outputConfig.output();
        }
        outputs[count++] = outputConfig.output();//application output last
        return new CompositeOutput(
                count == outputs.length ? outputs : Arrays.copyOf(outputs, count),
                appConfig.exceptionHandler()
        );
    }
}
