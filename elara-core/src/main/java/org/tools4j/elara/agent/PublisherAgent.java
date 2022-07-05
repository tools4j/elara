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
package org.tools4j.elara.agent;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.CommittedEventPoller;

import static java.util.Objects.requireNonNull;

/**
 * Agent to poll and publish events.
 * <p>
 * The agent invokes the output handler with committed events and replay flag during replay.  A tracking poller is used
 * to store the index of the last event passed to the handler.  A second poller is used to also pass replayed events to
 * the output handler.  Using a {@link CommittedEventPoller} as tracking poller guarantees that only committed events
 * are passed to the handler.
 */
public class PublisherAgent implements Agent {
    private final AgentStep publisherStep;

    public PublisherAgent(final AgentStep publisherStep) {
        this.publisherStep = requireNonNull(publisherStep);
    }

    @Override
    public int doWork() throws Exception {
        return publisherStep.doWork();
    }

    @Override
    public String roleName() {
        return "elara-pub";
    }
}
