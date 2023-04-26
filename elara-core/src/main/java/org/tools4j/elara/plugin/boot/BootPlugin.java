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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.Plugin.NullState;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin;

import static java.util.Objects.requireNonNull;

/**
 * A plugin that issues commands and events related to booting an elara application to indicate that the application has
 * been started and initialised.
 */
public class BootPlugin implements SystemPlugin<NullState> {

    public static final int DEFAULT_SOURCE_ID = -20;
    public static final BootPlugin DEFAULT = new BootPlugin(DEFAULT_SOURCE_ID);

    private final int sourceId;

    public BootPlugin(final int sourceId) {
        this.sourceId = sourceId;
    }

    @Override
    public ReservedPayloadType reservedPayloadType() {
        return ReservedPayloadType.BOOT;
    }

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
            public Input input(final BaseState baseState) {
                final long sourceSeq = 1 + baseState.lastAppliedCommandSequence(sourceId);
                return Input.single(sourceId, sourceSeq, new BootCommandInput());
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new BootCommandProcessor();
            }
        };
    }

}
