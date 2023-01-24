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
package org.tools4j.elara.plugin.heartbeat;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin;
import org.tools4j.elara.plugin.api.Plugin.NullState;
import org.tools4j.elara.plugin.base.BaseState;

import static java.util.Objects.requireNonNull;

/**
 * A plugin that issues heartbeat commands and optionally events that can be reacted upon.
 */
public class HeartbeatPlugin implements Plugin<NullState> {

    @Override
    public NullState defaultPluginState(final AppConfig appConfig) {
        return NullState.NULL;
    }

    @Override
    public Configuration configuration(final AppConfig appConfig, final NullState pluginState) {
        requireNonNull(appConfig);
        requireNonNull(pluginState);
        return new Configuration.Default() {
            @Override
            public Input[] inputs(final BaseState baseState) {
                return new Input[0];
            }

            @Override
            public Output output(final BaseState baseState) {
                return Output.NOOP;
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return null;
            }

            @Override
            public EventApplier eventApplier(final BaseState.Mutable baseState) {
                return EventApplier.NOOP;
            }
        };
    }
}
