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

import org.tools4j.elara.time.TimeSource;

/**
 * Transient state with information related to the sequencer engine that is not necessarily reflected in the app state
 * yet.
 * <p>
 * Note that transient state should not be used in the decision-making logic of the application, otherwise its state
 * will not be deterministic and cannot be reproduced through event replay.
 */
public interface TransientEngineState extends TransientState {
    /**
     * Returns the maximum event sequence number known to exist in the engine, possibly not yet received by this
     * application. Returns {@link BaseState#NIL_SEQUENCE} if nothing is known about the max available event, or no
     * events exist yet at all.
     *
     * @return the maximum event sequence know to exist, or {@link BaseState#NIL_SEQUENCE} if unavailable or unknown
     */
    long maxAvailableEventSequence();

    /**
     * Returns the event time of the newest event, or {@link TimeSource#MIN_VALUE} if nothing is known about the newest
     * event, or no events exist yet at all.
     * @return  the time of the event corresponding to {@link #maxAvailableEventSequence()}, or
     *          {@link TimeSource#MIN_VALUE} if unavailable
     */
    long newestEventTime();

    /**
     * Returns the maximum source sequence number known to exist in the engine for the specified source, possibly not
     * yet received by this application. Returns {@link BaseState#NIL_SEQUENCE} if nothing is known about the max
     * available source sequence, or no events exist yet for the specified source.
     *
     * @param sourceId the source ID
     * @return  the maximum source sequence know to exist for this source ID, or {@link BaseState#NIL_SEQUENCE} if
     *          unavailable or unknown
     */
    long maxAvailableSourceSequence(int sourceId);
}
