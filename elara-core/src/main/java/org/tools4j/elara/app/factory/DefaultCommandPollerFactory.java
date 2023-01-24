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

import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.config.CommandStoreConfig;
import org.tools4j.elara.handler.CommandPollerHandler;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.CommandPollerStep;
import org.tools4j.elara.store.MessageStore.Poller;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultCommandPollerFactory implements CommandPollerFactory {

    private final CommandStoreConfig commandStoreConfig;
    private final Supplier<? extends ProcessorFactory> processorSingletons;

    public DefaultCommandPollerFactory(final CommandStoreConfig commandStoreConfig,
                                       final Supplier<? extends ProcessorFactory> processorSingletons) {
        this.commandStoreConfig = requireNonNull(commandStoreConfig);
        this.processorSingletons = requireNonNull(processorSingletons);
    }

    @Override
    public AgentStep commandPollerStep() {
        final Poller commandStorePoller;
        switch (commandStoreConfig.commandPollingMode()) {
            case REPLAY_ALL:
                commandStorePoller = commandStoreConfig.commandStore().poller();
                break;
            case FROM_LAST:
                commandStorePoller = commandStoreConfig.commandStore().poller(CommandPollingMode.DEFAULT_POLLER_ID);
                break;
            case FROM_END:
                commandStorePoller = commandStoreConfig.commandStore().poller();
                commandStorePoller.moveToEnd();
                break;
            case NO_STORE:
                return AgentStep.NOOP;
            default:
                throw new IllegalArgumentException("Unsupported command polling mode: " + commandStoreConfig.commandPollingMode());
        }
        return new CommandPollerStep(commandStorePoller, new CommandPollerHandler(processorSingletons.get().commandHandler()));
    }

}
