/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.type;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.factory.AllInOneAppFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.store.MessageStore;

import static java.util.Objects.requireNonNull;

final class AllInOneAppContextImpl extends AbstractAppContext<AllInOneAppContextImpl> implements AllInOneAppContext {

    private CommandProcessor commandProcessor;
    private EventApplier eventApplier;
    private MessageStore commandStore;
    private CommandPollingMode commandPollingMode = CommandPollingMode.NO_STORE;
    private MessageStore eventStore;

    @Override
    protected AllInOneAppContextImpl self() {
        return this;
    }

    @Override
    public CommandProcessor commandProcessor() {
        return commandProcessor;
    }

    @Override
    public AllInOneAppContext commandProcessor(final CommandProcessor commandProcessor) {
        this.commandProcessor = requireNonNull(commandProcessor);
        return this;
    }

    @Override
    public EventApplier eventApplier() {
        return eventApplier;
    }

    @Override
    public AllInOneAppContext eventApplier(final EventApplier eventApplier) {
        this.eventApplier = requireNonNull(eventApplier);
        return this;
    }

    @Override
    public MessageStore commandStore() {
        return commandStore;
    }

    @Override
    public AllInOneAppContext commandStore(final MessageStore commandStore) {
        this.commandStore = commandStore;//nullable
        if (commandStore == null) {
            commandPollingMode = CommandPollingMode.NO_STORE;
        } else if (commandPollingMode == CommandPollingMode.NO_STORE) {
            commandPollingMode = CommandPollingMode.FROM_END;
        }
        return this;
    }

    @Override
    public CommandPollingMode commandPollingMode() {
        return commandPollingMode;
    }

    @Override
    public AllInOneAppContext commandPollingMode(final CommandPollingMode mode) {
        this.commandPollingMode = requireNonNull(mode);
        return this;
    }

    @Override
    public MessageStore eventStore() {
        return eventStore;
    }

    @Override
    public AllInOneAppContext eventStore(final MessageStore eventStore) {
        this.eventStore = requireNonNull(eventStore);
        return this;
    }

    @Override
    public AllInOneAppContextImpl populateDefaults() {
        return super.populateDefaults();
    }

    @Override
    public AllInOneAppContext populateDefaults(final AllInOneApp app) {
        return this
                .commandProcessor(app)
                .eventApplier(app)
                .output(app instanceof Output ? ((Output)app) : Output.NOOP)
                .populateDefaults();
    }

    @Override
    public void validate() {
        if (commandProcessor() == null) {
            throw new IllegalArgumentException("Command processor must be set");
        }
        if (eventApplier() == null) {
            throw new IllegalArgumentException("Event applier must be set");
        }
        if (commandStore() == null && commandPollingMode() != CommandPollingMode.NO_STORE) {
            throw new IllegalArgumentException("Command store must be set unless command polling mode is NO_STORE");
        }
        if (eventStore() == null) {
            throw new IllegalArgumentException("Event store must be set");
        }
        super.validate();
    }

    @Override
    public Agent createAgent() {
        populateDefaults().validate();
        return new AllInOneAppFactory(this).agent();
    }
}
