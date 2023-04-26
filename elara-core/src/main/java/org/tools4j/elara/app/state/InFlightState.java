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
package org.tools4j.elara.source;

import org.tools4j.elara.command.Command;
import org.tools4j.elara.flyweight.EventType;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.sequence.SequenceGenerator;

/**
 * Application state that provides information about own commands that are still {@link #hasInFlightCommand() in-flight}.
 * A command is in-flight if the corresponding event has not been received back.
 */
public interface InFlightState {
    /**
     * Value returned by {@link CommandSendingState#sourceSequence()} if no command has been sent yet, and by
     * {@link EventProcessingState#sourceSequence()} and {@link EventProcessingState#eventSequence()} if no event has
     * been received yet that corresponds to a command sent by this application.
     */
    long NIL_SEQUENCE = SequenceGenerator.NIL_SEQUENCE;

    /**
     * Returns information about the command most recently sent by this application.
     * @return information about the last sent command
     */
    CommandSendingState commandLastSent();

    /**
     * Returns information about the most recently processed event corresponding to a command sent by this application.
     * Note that this includes the event currently processed if that is an event from a command sent by this
     * application.
     * @return  information about the event last received and processed, considering only events from commands sent by
     *          this application
     */
    EventProcessingState eventLastProcessed();

    /**
     * True if commands are currently in-flight, that is, if at least one command has been sent whose commit event was
     * not yet processed.
     * <p>
     * Note that
     * {@link org.tools4j.elara.app.handler.CommandProcessor#onCommand(Command, EventRouter) command processing}
     * can yield more than one event. The commit event is the last event that corresponds to the same command.
     * <p>
     * This method returns false if no commands are currently in-flight, or if the currently processed event is the
     * commit event of the last in-flight command.
     *
     * @return true if at least on command is in-flight whose commit event was not yet processed
     */
    boolean hasInFlightCommand();

    /**
     * Information about the {@link #commandLastSent() command last sent} by this application
     */
    interface CommandSendingState {
        /**
         * Returns the source sequence of the most recently sent command, or {@link #NIL_SEQUENCE} if no command was
         * ever sent yet.
         * @return the source sequence of the command, or {@link #NIL_SEQUENCE} if unavailable
         */
        long sourceSequence();

        /**
         * Returns the sending time of the most recently sent command, or
         * {@link org.tools4j.elara.time.TimeSource#MIN_VALUE TimeSource.MIN_VALUE} if no command was ever sent yet.
         * @return the sending time of the command
         */
        long sendingTime();
    }

    /**
     * Information about the {@link #eventLastProcessed() last processed event} from a command sent by this application
     */
    interface EventProcessingState {
        /**
         * Returns the source sequence of the most recently processed event of an own command, or {@link #NIL_SEQUENCE}
         * if no event corresponding to an own command has been processed yet.
         *
         * @return the source sequence of the event, or {@link #NIL_SEQUENCE} if unavailable
         */
        long sourceSequence();

        /**
         * Returns the event sequence of the most recently processed event of an own command, or {@link #NIL_SEQUENCE}
         * if no event corresponding to an own command has been processed yet.
         *
         * @return the event sequence of the event, or {@link #NIL_SEQUENCE} if unavailable
         */
        long eventSequence();

        /**
         * Returns the event index of the most recently processed event of an own command, or -1 if no event
         * corresponding to an own command has been processed yet.
         *
         * @return the event index, or -1 if unavailable
         */
        int eventIndex();

        /**
         * Returns the event type of the most recently processed event of an own command, or null if no event
         * corresponding to an own command has been processed yet.
         *
         * @return the event type, or null if unavailable
         */
        EventType eventType();
    }
}
