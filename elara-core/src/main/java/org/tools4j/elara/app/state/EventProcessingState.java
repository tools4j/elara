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
     * Returns the event state for the given source ID, or null if no events from this source have been processed yet.
     *
     * @param sourceId the source ID for events from a particular command source
     * @return the event state for the given source ID, or null if unavailable
     */
    EventState lastProcessedEvent(int sourceId);

    /**
     * Returns transient state with non-deterministic information about in-flight commands which have been sent but
     * whose event(s) have not been received back yet.
     * @return transient state with information about commands and command sending
     */
    TransientInFlightState transientInFlightState();

    /**
     * Returns transient non-deterministic state related to the sequencer engine that is not necessarily reflected in
     * the application state yet.
     *
     * @return transient information from the engine
     */
    TransientEngineState transientEngineState();
}
