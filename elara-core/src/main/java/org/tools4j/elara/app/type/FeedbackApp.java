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

import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.stream.MessageReceiver;

import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public interface FeedbackApp extends EventProcessor {

    @Override
    void onEvent(Event event, CommandContext commandContext, CommandSender sender);

    default ElaraRunner launch(final MessageStore messageStore) {
        requireNonNull(messageStore);
        return launch(config -> config.eventStore(messageStore));
    }

    default ElaraRunner launch(final MessageReceiver eventReceiver) {
        requireNonNull(eventReceiver);
        return launch(config -> config.eventReceiver(eventReceiver));
    }

    default ElaraRunner launch(final Consumer<? super FeedbackAppConfigurator> configurator) {
        final FeedbackAppConfigurator config = FeedbackAppConfig.configure();
        configurator.accept(config);
        config.populateDefaults(this);
        return Elara.launch(config);
    }
}
