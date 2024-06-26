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
import org.tools4j.elara.app.factory.PassthroughAppFactory;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.store.MessageStore;

import static java.util.Objects.requireNonNull;

final class PassthroughAppConfiguratorImpl extends AbstractAppConfigurator<PassthroughAppConfiguratorImpl> implements PassthroughAppConfigurator {

    private MessageStore eventStore;

    @Override
    protected PassthroughAppConfiguratorImpl self() {
        return this;
    }

    @Override
    public MessageStore eventStore() {
        return eventStore;
    }

    @Override
    public PassthroughAppConfigurator eventStore(final MessageStore eventStore) {
        this.eventStore = requireNonNull(eventStore);
        return this;
    }

    @Override
    public PassthroughAppConfiguratorImpl populateDefaults() {
        return super.populateDefaults();
    }

    @Override
    public PassthroughAppConfigurator populateDefaults(final PassthroughApp app) {
        return this
                .output(app instanceof Output ? ((Output)app) : Output.NOOP)
                .populateDefaults();
    }

    @Override
    public void validate() {
        if (eventStore() == null) {
            throw new IllegalStateException("Event store must be set");
        }
        super.validate();
    }

    @Override
    public Agent createAgent() {
        populateDefaults().validate();
        return new PassthroughAppFactory(this).agent();
    }
}
