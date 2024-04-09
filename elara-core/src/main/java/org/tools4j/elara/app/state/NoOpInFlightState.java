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

import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.flyweight.EventType;

/**
 * A no-OP implementation of {@link MutableInFlightState} for cases where in-flight state is not to be tracked to
 * optimize performance.
 */
public final class NoOpInFlightState implements MutableInFlightState {
    public static final NoOpInFlightState INSTANCE = new NoOpInFlightState();

    @Override
    public int inFlightCommands() {
        return 0;
    }

    @Override
    public int inFlightCommands(final int sourceId) {
        return 0;
    }

    @Override
    public int sourceId(final int index) {
        throw new IndexOutOfBoundsException("Invalid index " + index);
    }

    @Override
    public long sourceSequence(final int index) {
        throw new IndexOutOfBoundsException("Invalid index " + index);
    }

    @Override
    public long sendingTime(final int index) {
        throw new IndexOutOfBoundsException("Invalid index " + index);
    }

    @Override
    public boolean hasInFlightCommand() {
        return false;
    }

    @Override
    public boolean hasInFlightCommand(final int sourceId) {
        return false;
    }

    @Override
    public void onCommandSent(final int sourceId, final long sourceSequence, final long sendingTime) {
        //no-op
    }

    @Override
    public void onEvent(final Event evt) {
        //no-op
    }

    @Override
    public void onEvent(final int srcId, final long srcSeq, final long evtSeq, final int index, final EventType evtType, final long evtTime, final int payloadType) {
        //no-op
    }
}
