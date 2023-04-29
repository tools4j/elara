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

import org.tools4j.elara.app.config.InputConfig;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.PluginSpecification.Installer;

import static java.util.Objects.requireNonNull;

public class DefaultInputFactory implements InputFactory {
    private final InputConfig inOutConfig;
    private final BaseState baseState;
    private final Installer[] plugins;

    public DefaultInputFactory(final InputConfig inOutConfig,
                               final BaseState baseState,
                               final Installer[] plugins) {
        this.inOutConfig = requireNonNull(inOutConfig);
        this.baseState = requireNonNull(baseState);
        this.plugins = requireNonNull(plugins);
    }

    @Override
    public Input input() {
        Input input = inOutConfig.input();
        for (final Installer plugin : plugins) {
            input = Input.roundRobin(input, plugin.input(baseState));
        }
        return input;
    }
}
