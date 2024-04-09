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
package org.tools4j.elara.source;

import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.EventProcessingState;
import org.tools4j.elara.app.state.EventState;
import org.tools4j.elara.app.state.TransientCommandSourceState;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;

/**
 * A command source bundles information about command sending from a particular source identified through a source ID.
 * Commands are sent through the {@link #commandSender() command sender} and all commands from a source are sequenced
 * with source sequence numbers.  Sent commands are tracked and marked as {@link #hasInFlightCommand() in-flight} until
 * the event (or events) resulting from that command have been received back.
 *
 * @see CommandContext
 */
public interface CommandSource {
    /**
     * Returns the source ID for commands associated with this command source.
     * @return the source ID for commands from this source
     */
    int sourceId();

    /**
     * True if commands from this source are currently in-flight, that is, if at least one command has been sent whose
     * commit event was not yet processed.  This may be useful to defer event processing until all events have from sent
     * commands have been received back and the application state updated.
     * <p>
     * Note that {@link CommandProcessor#onCommand(Command, EventRouter) command processing} can yield more than one
     * event. The commit event is the last event that corresponds to the same command.
     * <p>
     * This method returns false if no commands from this source are currently in-flight, or if the currently processed
     * event is the commit event of the last in-flight command.
     *
     * @return true if at least one command is in-flight whose commit event was not yet processed
     */
    boolean hasInFlightCommand();

    /**
     * Returns the sender for sending commands from this command source.
     * @return the command sender for this source
     */
    CommandSender commandSender();

    /**
     * Returns transient state associated with command sending that is not reflected by events, hence it is
     * non-deterministic. Transient command state is used by {@link CommandContext} to determine if commands are
     * currently {@link CommandContext#hasInFlightCommand() in-flight}, meaning that some events are still missing for
     * commands that have been sent.
     * <p>
     * Note however that transient state should not be used in the decision-making logic of the application, otherwise
     * its state will not be deterministic and cannot be reproduced through event replay.
     *
     * @return transient information about command sending
     * @see CommandContext#hasInFlightCommand()
     */
    TransientCommandSourceState transientCommandSourceState();

    /**
     * Returns information about the most recently processed event corresponding to a command sent by this source.
     * Note that this includes the event currently processed if originates from this source.
     * <p>
     * Application types using {@link BaseState} instead of {@link EventProcessingState} to track events (typically
     * sequencer and command processor apps), not all elements in the returned event state will be populated (as
     * indicated in the description of the {@link EventState} methods).
     *
     * @return  information about the event last received and processed, considering only events from commands sent by
     *          this source
     */
    EventState eventLastProcessed();
}
