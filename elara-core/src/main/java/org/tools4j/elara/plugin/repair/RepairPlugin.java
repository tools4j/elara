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
package org.tools4j.elara.plugin.repair;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.EventStoreConfig;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.plugin.api.Plugin.NullState;
import org.tools4j.elara.plugin.api.ReservedPayloadType;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.store.EventStoreRepairer;
import org.tools4j.elara.store.MessageStore;

import java.io.IOException;

/**
 * Plugin that repairs an event store on startup if necessary by appending a rollback event if an uncommitted command is
 * detected at the end of the store.
 */
public enum RepairPlugin implements SystemPlugin<NullState> {
    INSTANCE;

    @Override
    public NullState defaultPluginState(final AppConfig appConfig) {
        return NullState.NULL;
    }

    @Override
    public Configuration configuration(final AppConfig appConfig, final NullState pluginState) {
        if (appConfig instanceof EventStoreConfig) {
            repairEventStoreIfNeeded(((EventStoreConfig) appConfig).eventStore(), appConfig.exceptionHandler());
        }
        return NO_CONFIGURATION;
    }

    @Override
    public ReservedPayloadType reservedPayloadType() {
        return ReservedPayloadType.NONE;
    }

    private void repairEventStoreIfNeeded(final MessageStore eventStore,
                                          final ExceptionHandler exceptionHandler) {
        final EventStoreRepairer eventStoreRepairer = new EventStoreRepairer(eventStore);
        if (eventStoreRepairer.isCorrupted()) {
            exceptionHandler.handleException("Repairing corrupted event store",
                    new IOException("Corrupted event store: " + eventStore));
            eventStoreRepairer.repair();
        }
    }
}
