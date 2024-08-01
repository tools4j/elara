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

import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.send.CommandSender;

/**
 * Transient state with information related to command sending, for instance about in-flight commands which have been
 * sent but whose corresponding event (or events) have not been received back yet.
 * <p>
 * Note that transient state should not be used in the decision-making logic of the application, otherwise its state
 * will not be deterministic and cannot be reproduced through event replay.
 */
public interface TransientInFlightState extends TransientState {
    /**
     * Returns the number of in-flight commands.
     * @return the number of commands currently in-flight
     */
    int inFlightCommands();

    /**
     * Returns the number of in-flight commands from the source specified by the given source ID.
     * @param sourceId the source ID for which to check existence of in-flight commands
     * @return the number of commands currently in-flight from the source
     */
    int inFlightCommands(int sourceId);

    /**
     * Returns the total number of bytes of all in-flight commands.
     * @return the byte size of all in-flight commands.
     */
    long inFlightBytes();

    /**
     * Returns the source ID of the in-flight command specified by {@code index}.
     * @param index the index, a value in {@code [0..(n-1)]} where {@code n} is the number of in-flight commands
     * @return the source ID of the command
     * @throws IndexOutOfBoundsException if index is not valid
     * @see #inFlightCommands()
     */
    int sourceId(int index);

    /**
     * Returns the sequence number of the in-flight command specified by {@code index}.
     * @param index the index, a value in {@code [0..(n-1)]} where {@code n} is the number of in-flight commands
     * @return the source sequence number of the command
     * @throws IndexOutOfBoundsException if index is not valid
     * @see #inFlightCommands()
     */
    long sourceSequence(int index);

    /**
     * Returns the sending time of the in-flight command specified by {@code index}.
     * @param index the index, a value in {@code [0..(n-1)]} where {@code n} is the number of in-flight commands
     * @return the sending time of the command
     * @throws IndexOutOfBoundsException if index is not valid
     * @see #inFlightCommands()
     */
    long sendingTime(int index);

    /**
     * True if any commands are currently in-flight, that is, if at least one command has been sent whose commit event
     * was not yet processed.  This information may be useful for instance to defer event processing until all events
     * from sent commands have been received back and the application state has been updated.
     * <p>
     * Note that {@link CommandProcessor#onCommand(Command, EventRouter) command processing} can yield more than one
     * event. The commit event is the last event that corresponds to the same command. This method returns false if
     * no commands are currently in-flight, or if the currently processed event is the commit event of the last
     * in-flight command.
     *
     * @return true if at {@link #inFlightCommands()} is non-zero
     * @see #inFlightCommands()
     * @see CommandSender#source()
     */
    default boolean hasInFlightCommand() {
        return inFlightCommands() > 0;
    }

    /**
     * True if any commands are currently in-flight from the source specified by the given source ID.
     *
     * @param sourceId the source ID for which to check existence of in-flight commands 
     * @return true if at least one command source has a command in-flight
     * @see #hasInFlightCommand()
     * @see #inFlightCommands(int)
     */
    default boolean hasInFlightCommand(final int sourceId) {
        return inFlightCommands(sourceId) > 0;
    }
}
