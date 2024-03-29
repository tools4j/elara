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
package org.tools4j.elara.app.handler;

import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.EventProcessingState;
import org.tools4j.elara.app.state.EventState;
import org.tools4j.elara.app.state.TransientCommandState;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.route.EventRouter;

/**
 * Tracks sent commands and processing of corresponding events and provides information about
 * {@link #hasInFlightCommand() in-flight} commands whose corresponding event (or events) have not been received back
 * yet.
 *
 * @see #hasInFlightCommand()
 */
public interface CommandTracker {
    /**
     * Source ID of all commands and events tracked by this command tracker
     * @return source ID of all tracked commands and events
     */
    int sourceId();

    /**
     * True if commands are currently in-flight, that is, if at least one command has been sent whose commit event was
     * not yet processed.  This may be useful to defer event processing until all events have from sent commands have
     * been received back and the application state updated.
     * <p>
     * Note that
     * {@link org.tools4j.elara.app.handler.CommandProcessor#onCommand(Command, EventRouter) command processing}
     * can yield more than one event. The commit event is the last event that corresponds to the same command.
     * <p>
     * This method returns false if no commands are currently in-flight, or if the currently processed event is the
     * commit event of the last in-flight command.
     *
     * @return true if at least one command is in-flight whose commit event was not yet processed
     */
    boolean hasInFlightCommand();

    /**
     * Returns transient state associated with command sending that is not reflected by events, hence it is
     * non-deterministic. State from this class can be used for instance to determine that commands are currently
     * {@link #hasInFlightCommand() in-flight}.
     * <p>
     * Note however that transient state should not be used in the decision-making logic of the application, otherwise
     * its state will not be deterministic and cannot be reproduced through event replay.
     *
     * @return transient information about command sending
     * @see TransientCommandState
     * @see #hasInFlightCommand()
     */
    TransientCommandState transientCommandState();

    /**
     * Returns information about the most recently processed event corresponding to a command sent by this application.
     * Note that this includes the event currently processed if originates from the {@link #sourceId() source} tracked
     * by this command tracker.
     * <p>
     * Application types using {@link BaseState} instead of {@link EventProcessingState} to track events (typically
     * sequencer and command processor apps), not all elements in the returned event state will be populated (as
     * indicated in the description of the {@link EventState} methods).
     *
     * @return  information about the event last received and processed, considering only events from commands sent by
     *          this application
     */
    EventState eventLastProcessed();
}
