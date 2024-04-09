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

import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.stream.MessageReceiver;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public interface PublisherApp extends Output {

    @Override
    Ack publish(Event event, boolean replay, int retry);

    default ElaraRunner launch(final MessageStore eventStore) {
        requireNonNull(eventStore);
        return launch(config -> config.eventStore(eventStore));
    }

    default ElaraRunner launch(final MessageStore.Poller eventStorePoller) {
        requireNonNull(eventStorePoller);
        return launch(config -> config.eventStore(eventStorePoller));
    }

    default ElaraRunner launch(final MessageReceiver eventReceiver) {
        requireNonNull(eventReceiver);
        return launch(config -> config.eventReceiver(eventReceiver));
    }

    default ElaraRunner launch(final Consumer<? super PublisherAppConfigurator> configurator) {
        final PublisherAppConfigurator config = PublisherAppConfig.configure();
        configurator.accept(config);
        config.populateDefaults(this);
        return Elara.launch(config);
    }
}
