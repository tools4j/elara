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
package org.tools4j.elara.loop;

import org.agrona.DirectBuffer;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.store.CommittedEventPoller;
import org.tools4j.elara.store.MessageStore;
import org.tools4j.elara.store.MessageStore.Poller;
import org.tools4j.elara.stream.MessageStream.Handler;
import org.tools4j.elara.stream.MessageStream.Handler.Result;
import org.tools4j.nobark.loop.Step;

import static java.util.Objects.requireNonNull;

/**
 * Step that invokes output handler with committed events and replay flag during replay.  A tracking poller is used to
 * store the index of the last event passed to the handler.  A second poller is used to also pass replayed events to the
 * output handler.  Using a {@link CommittedEventPoller} as tracking poller guarantees that only committed events are
 * passed to the handler.
 */
public class OutputStep implements Step {

    public static final String DEFAULT_POLLER_ID = "elara-out";

    private final OutputHandler handler;
    private final Poller poller;
    private final Handler replayHandler = buffer -> onMessage(buffer, true);
    private final Handler defaultHandler = buffer -> onMessage(buffer, false);
    private final FlyweightEvent flyweightEvent = new FlyweightEvent();
    private Poller replayPoller;
    private int retry;

    public OutputStep(final OutputHandler handler, final MessageStore messageStore) {
        this(handler, new CommittedEventPoller(messageStore), null);
    }

    public OutputStep(final OutputHandler handler, final MessageStore messageStore, final String id) {
        this(handler, new CommittedEventPoller(messageStore, id), messageStore.poller());
    }

    private OutputStep(final OutputHandler handler, final Poller poller, final Poller replayPoller) {
        this.handler = requireNonNull(handler);
        this.poller = requireNonNull(poller);
        this.replayPoller = replayPoller;//nullable
    }

    @Override
    public boolean perform() {
        if (replayPoller != null) {
            if (replayPoller.entryId() < poller.entryId()) {
                return replayPoller.poll(replayHandler) > 0;
            }
            replayPoller = null;
        }
        return poller.poll(defaultHandler) > 0;
    }

    private Result onMessage(final DirectBuffer message, final boolean replay) {
        flyweightEvent.init(message, 0);
        try {
            final Ack ack = handler.publish(flyweightEvent, replay, retry);
            if (Ack.RETRY != ack) {
                retry = 0;
                return Result.POLL;
            }
            retry++;
            return Result.PEEK;
        } finally {
            flyweightEvent.reset();
        }
    }

}
