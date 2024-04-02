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
package org.tools4j.elara.app.state;

import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.type.PassthroughApp;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.send.CommandPassthroughSender;

/**
 * A minimalistic event applier using only basic event header information when applying an event. It is used by
 * {@link PassthroughApp} and {@link CommandPassthroughSender} as an optimisation if only the base
 * {@link PassthroughState} needs to be updated by events.
 */
@FunctionalInterface
public interface PassthroughEventApplier extends EventApplier {

    //NOTE: this method may not be invoked at all, so do not override it!
    @Override
    default void onEvent(final Event evt) {
        onEvent(evt.sourceId(), evt.sourceSequence(), evt.eventSequence(), evt.eventIndex(), evt.eventType(),
                evt.eventTime(), evt.payloadType());
    }

    void onEvent(int srcId, long srcSeq, long evtSeq, int index, EventType evtType, long evtTime, int payloadType);
}
