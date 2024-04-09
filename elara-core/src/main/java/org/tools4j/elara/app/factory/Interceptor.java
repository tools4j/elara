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
package org.tools4j.elara.app.factory;

import java.util.function.Supplier;

public interface Interceptor {

    Interceptor NOOP = new Interceptor() {};

    default AppFactory appFactory(final Supplier<? extends AppFactory> singletons) {
        return null;
    }

    default StateFactory stateFactory(final Supplier<? extends StateFactory> singletons) {
        return null;
    }

    default AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> singletons) {
        return null;
    }

    default SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
        return null;
    }

    default CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
        return null;
    }

    default CommandProcessorFactory commandProcessorFactory(final Supplier<? extends CommandProcessorFactory> singletons) {
        return null;
    }

    default ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
        return null;
    }

    default InputFactory inputFactory(final Supplier<? extends InputFactory> singletons) {
        return null;
    }

    default OutputFactory outputFactory(final Supplier<? extends OutputFactory> singletons) {
        return null;
    }

    default PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
        return null;
    }

    default CommandSenderFactory commandSenderFactory(final Supplier<? extends CommandSenderFactory> singletons) {
        return null;
    }

    default EventSubscriberFactory eventSubscriberFactory(final Supplier<? extends EventSubscriberFactory> singletons) {
        return null;
    }

    default EventProcessorFactory eventProcessorFactory(final Supplier<? extends EventProcessorFactory> singletons) {
        return null;
    }
}
