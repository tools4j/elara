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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.init.ExecutionType;
import org.tools4j.elara.input.CommandStoreReceiver;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin.NullState;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.plugin.api.TypeRange;
import org.tools4j.elara.plugin.base.BaseState;
import org.tools4j.elara.step.AgentStep;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.boot.BootCommands.SIGNAL_APP_INITIALISATION_START;

/**
 * A plugin that issues commands and events related to booting an elara application to indicate that the application has
 * been started and initialised.
 */
public class BootPlugin implements SystemPlugin<NullState> {

    public static final int DEFAULT_COMMAND_SOURCE = -20;
    public static final BootPlugin DEFAULT = new BootPlugin(DEFAULT_COMMAND_SOURCE, new SimpleSequenceGenerator(System.currentTimeMillis()));

    private final int commandSource;
    private final SequenceGenerator sequenceGenerator;

    public BootPlugin(final int commandSource, final SequenceGenerator sequenceGenerator) {
        this.commandSource = commandSource;
        this.sequenceGenerator = requireNonNull(sequenceGenerator);
    }

    @Override
    public TypeRange typeRange() {
        return TypeRange.BOOT;
    }

    @Override
    public NullState defaultPluginState() {
        return NullState.NULL;
    }

    @Override
    public Configuration configuration(final org.tools4j.elara.init.Configuration appConfig, final NullState pluginState) {
        requireNonNull(appConfig);
        requireNonNull(pluginState);
        return new Configuration.Default() {
            @Override
            public AgentStep step(final BaseState baseState, final ExecutionType executionType) {
                if (executionType == ExecutionType.INIT_ONCE_ONLY) {
                    return () -> {
                        appendAppInitStartCommand(appConfig);
                        return 1;
                    };
                }
                return AgentStep.NO_OP;
            }

            @Override
            public Output output(final BaseState baseState) {
                return new BootOutput();
            }

            @Override
            public CommandProcessor commandProcessor(final BaseState baseState) {
                return new BootCommandProcessor();
            }
        };
    }

    private void appendAppInitStartCommand(final org.tools4j.elara.init.Configuration appConfig) {
        final Receiver receiver = new CommandStoreReceiver(appConfig.timeSource(), appConfig.commandStore().appender());
        receiver.receiveMessageWithoutPayload(commandSource, sequenceGenerator.nextSequence(), SIGNAL_APP_INITIALISATION_START);
    }
}
