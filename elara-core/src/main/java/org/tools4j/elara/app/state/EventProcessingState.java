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

/**
 * Extended version of {@link BaseState} providing information about the last processed event for any given source ID.
 */
public interface EventProcessingState extends BaseState {
    /**
     * Returns the highest event sequence that is available from the sequencer, but has not necessarily been
     * {@link #lastAppliedEventSequence() applied} yet
     *
     * @return the event sequence of the last available event, or {@link #NIL_SEQUENCE} if no events are available
     */
    long maxAvailableEventSequence();

    /**
     * Returns the event state for the given source ID, or null if no events from this source have been processed yet.
     *
     * @param sourceId the source ID for events from a particular command source
     * @return the event state for the given source ID, or null if unavailable
     */
    EventState lastProcessedEvent(int sourceId);

    /**
     * Returns the transient part of the base state with information about in-flight commands.
     * @return transient state with information about in-flight commands
     */
    TransientInFlightState transientInFlightState();

}
