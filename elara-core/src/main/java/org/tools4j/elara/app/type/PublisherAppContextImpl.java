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
package org.tools4j.elara.app.type;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.app.handler.EventProcessor;

final class PublisherAppContextImpl extends AbstractEventStreamContext<PublisherAppContextImpl> implements PublisherAppContext {

    public static final String DEFAULT_THREAD_NAME = "elara-pub";

    @Override
    protected PublisherAppContextImpl self() {
        return this;
    }

    @Override
    public PublisherAppContext populateDefaults() {
        if (eventProcessor() == null) {
            eventProcessor(EventProcessor.NOOP);
        }
        return populateDefaults(DEFAULT_THREAD_NAME);
    }

    @Override
    public PublisherAppContext populateDefaults(final PublisherApp app) {
        return this
                .output(app)
                .populateDefaults();
    }

    @Override
    public Agent createAgent() {
        populateDefaults().validate();
        throw new IllegalArgumentException("Implement publisher agent");//FIXME implement publisher agent
    }
}
