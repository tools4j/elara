/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.send;

import org.tools4j.elara.event.Event;
import org.tools4j.elara.time.TimeSource;

/**
 * Application state that provides information about own commands that are still {@link #hasInFlightCommand() in-flight}.
 * A command is in-flight if the corresponding event has not been received back.  In this situation event
 * {@link org.tools4j.elara.application.EventProcessor#process(Event, EventContext, InFlightState, CommandSender) processing}
 * may be
 * {@link org.tools4j.elara.application.EventProcessor.Ack#DEFERRED deferred} until all events have been received and
 * the applications state has been brought up-to-date.
 * <p>
 * See also {@link org.tools4j.elara.application.EventProcessor EventProcessor} and {@link EventContext} description for
 * more information.
 */
public interface InFlightState {

    /**
     * Value returned by {@link CommandSendingState#sequence()} if no command has been sent yet, and by
     * {@link EventProcessingState#sequence()} if no event has been received yet that corresponds to a command sent by
     * this application.
     */
    long SEQUENCE_UNAVAILABLE = -1;

    /**
     * Returns information about the command most recently sent by this application.
     * @return information about the most recently sent command
     */
    CommandSendingState mostRecentlySentCommand();

    /**
     * Returns information about the command most recently processed event corresponding to a command sent by this
     * application; this excludes the currently processed event.
     * @return most recently received and processed event corresponding to an application command
     */
    EventProcessingState mostRecentlyProcessedEvent();

    /**
     * True if commands are currently in-flight, that is, if at least one command has been sent whose commit event was
     * not yet processed.
     * <p>
     * Note that commands when processed in the processor app can yield more than one event; the commit event is the
     * last event that corresponds to the same command.
     *
     * @return true if at least on command is in-flight whose commit event was not yet processed
     */
    boolean hasInFlightCommand();

    /** Information about the {@link #mostRecentlySentCommand() most recently sent command} */
    interface CommandSendingState {
        /**
         * Returns the sequence of the most recently sent command, or {@link #SEQUENCE_UNAVAILABLE} if no command was
         * ever sent yet.
         * @return the sequence of the command, or {@link #SEQUENCE_UNAVAILABLE} if unavailable
         */
        long sequence();
        /**
         * Returns the time of the event that triggered processing of the command most recently sent, or
         * {@link org.tools4j.elara.time.TimeSource#MIN_VALUE TimeSource.MIN_VALUE} if no command was ever sent yet.
         * @return the time of the event that triggered the command
         */
        long time();

        /** Returned by {@link #mostRecentlySentCommand()} if no command has been sent yet. */
        CommandSendingState UNAVAILABLE = new CommandSendingState() {
            @Override
            public long sequence() {
                return SEQUENCE_UNAVAILABLE;
            }

            @Override
            public long time() {
                return TimeSource.MIN_VALUE;
            }
        };
    }

    /** Information about the {@link #mostRecentlyProcessedEvent() most recently processed event} of an own command */
    interface EventProcessingState {
        /**
         * Returns the sequence of the most recently processed event of an own command, or {@link #SEQUENCE_UNAVAILABLE}
         * if no event corresponding to an own command has been processed yet.
         *
         * @return the sequence of the event, or {@link #SEQUENCE_UNAVAILABLE} if unavailable
         */
        long sequence();
        /**
         * Returns the time of the most recently processed event of an own command, or
         * {@link org.tools4j.elara.time.TimeSource#MIN_VALUE TimeSource.MIN_VALUE} if no event corresponding to an own 
         * command has been processed yet.
         *
         * @return the time of the event
         */
        long time();
        /**
         * Returns the index of the most recently processed event of an own command, or -1 if no event corresponding to
         * an own command has been processed yet.
         *
         * @return the event index, or -1 if unavailable
         */
        int index();
        /**
         * Returns true if the most recently processed event was a commit event, and false otherwise.
         * <p>
         * Note that commands when processed in the processor app can yield more than one event; the commit event is the
         * last event that corresponds to the same command.
         *
         * @return true if a last processed event is available and is a commit event
         */
        boolean isCommit();

        /** Returned by {@link #mostRecentlyProcessedEvent()} if no event corresponding to an own command has been processed yet. */
        EventProcessingState UNAVAILABLE = new EventProcessingState() {
            @Override
            public long sequence() {
                return SEQUENCE_UNAVAILABLE;
            }

            @Override
            public long time() {
                return TimeSource.MIN_VALUE;
            }

            @Override
            public int index() {
                return -1;
            }

            @Override
            public boolean isCommit() {
                return false;
            }
        };
    }

    /** Interface extension providing default implementations */
    interface Default extends InFlightState {
        @Override
        default boolean hasInFlightCommand() {
            final CommandSendingState command = mostRecentlySentCommand();
            if (command == CommandSendingState.UNAVAILABLE) {
                return false;
            }
            final EventProcessingState event = mostRecentlyProcessedEvent();
            return command.sequence() > event.sequence() || !event.isCommit();
        }
    }
}
