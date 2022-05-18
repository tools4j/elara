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
package org.tools4j.elara.application;

import org.tools4j.elara.event.Event;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.EventContext;
import org.tools4j.elara.send.InFlightState;

/**
 * Event processor called to process events in a feedback app.  Note that own commands from a previous processing
 * invocation can still be {@link InFlightState#hasInFlightCommand() in-flight}.  This means the current state of this
 * application may not be up-to-date yet.  In the case of in-flight commands the application can choose to
 * {@link Ack#DEFERRED defer} an event so that it can be processed later once the state has been fully updated.
 * <p>
 * To understand the concept of in-flight commands, it is important to understand that application state can only be
 * modified when events are received and applied, otherwise determinism cannot be guaranteed.  For instance if we update
 * the state when sending a command, and the command is subsequently lost in transition, then subsequent decisions are
 * made on the basis of a corrupted application state.  The application state is corrupted because it can no longer be
 * reconstructed from events.
 */
@FunctionalInterface
public interface EventProcessor {
    /** Event processor that ignores all events */
    EventProcessor NOOP = (event, context, state, sender) -> Ack.IGNORED;

    /**
     * Ack returned when {@link #onEvent(Event, EventContext, InFlightState, CommandSender) processing} an event in a
     * feedback app.  If own commands are {@link InFlightState#hasInFlightCommand() in-flight} the application can
     * return {@link #DEFERRED} to re-process the event once the application state has been fully updated.
     */
    enum Ack {
        /** The event has been processed successfully */
        PROCESSED,
        /** Processing of the event has been deferred until the application state has been fully updated */
        DEFERRED,
        /** The event was ignored */
        IGNORED
    }

    /**
     * Invoked to process the given event.  This can be the most recently received event, or a deferred event that is
     * processed after bringing the application state up to date.  Returning {@link Ack#DEFERRED} if the no command was
     * in-flight has no effect and is ignored.
     *
     * @param event         the processed event (most recent or deferred)
     * @param context       context with information about processed and most-recent event
     * @param inFlightState in-flight state indicating whether this application is fully up-to-date or not
     * @param sender        handler to encode and send commands back to the sequencer
     * @return ack indicating the outcome of processing the command, in particular whether it was deferred or not
     */
    Ack onEvent(Event event, EventContext context, InFlightState inFlightState, CommandSender sender);
}
