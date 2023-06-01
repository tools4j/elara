/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.time.TimeSource;

/**
 * Information about the last processed event for a particular source ID as returned by {@link EventProcessingState}.
 * Note that if {@link BaseState} is used, some fields may not be populated when accessed through
 * {@link CommandTracker#eventLastProcessed()} as indicated by the method descriptions.
 */
public interface EventState {
    /**
     * Source ID of the event, provided even if no event was processed yet.
     * @return the event source ID, always provided
     */
    int sourceId();
    /**
     * Returns the number of events processed from the source associated with this event state.
     * <p>
     * Not always available: returns 1 for {@link BaseState} if at least one event was processed, and 0 otherwise.
     *
     * @return the number of events processed
     */
    long eventsProcessed();

    /**
     * Returns the source sequence of the most recently processed event, or {@link BaseState#NIL_SEQUENCE NIL_SEQUENCE}
     * if no event has been processed yet from the source associated with this event state.
     * <p>
     * Always available, also for {@link BaseState}.
     *
     * @return the source sequence of the event, or {@link BaseState#NIL_SEQUENCE NIL_SEQUENCE} if unavailable
     */
    long sourceSequence();

    /**
     * Returns the event sequence of the most recently processed event, or {@link BaseState#NIL_SEQUENCE NIL_SEQUENCE}
     * if no event has been processed yet from the source associated with this event state.
     * <p>
     * Not always available: returns {@link BaseState#MIN_SEQUENCE MIN_SEQUENCE} for {@link BaseState} if at least one
     * event was processed, and {@link BaseState#NIL_SEQUENCE NIL_SEQUENCE} otherwise.
     *
     * @return the event sequence of the event, or {@link BaseState#NIL_SEQUENCE NIL_SEQUENCE} if unavailable
     */
    long eventSequence();

    /**
     * Returns the event index of the most recently processed event, or -1 if no event has been processed yet from the
     * source associated with this event state.
     * <p>
     * Not always available: returns 0 for {@link BaseState} if at least one event was processed, and -1 otherwise.
     *
     * @return the event index, or -1 if unavailable
     */
    int eventIndex();

    /**
     * Returns the event type of the most recently processed event, or null if no event has been processed yet from the
     * source associated with this event state.
     * <p>
     * Not always available: returns {@link EventType#APP_COMMIT APP_COMMIT} for {@link BaseState} if at least one event
     * was processed, and null otherwise.
     *
     * @return the event type, or null if unavailable
     */
    EventType eventType();

    /**
     * Returns the event time of the most recently processed event, or {@link TimeSource#MIN_VALUE} if no event has been
     * processed yet from the source associated with this event state.
     * <p>
     * Not always available: returns {@link TimeSource#MIN_VALUE} for {@link BaseState}.
     *
     * @return the event time, or {@link TimeSource#MIN_VALUE} if unavailable
     */
    long eventTime();

    /**
     * Returns the payload type of the most recently processed event, or 0 if no event has been processed yet from the
     * source associated with this event state.
     * <p>
     * Not always available: returns 0 for {@link BaseState}.
     *
     * @return the event payload type, or 0 if unavailable
     */
    int payloadType();
}
