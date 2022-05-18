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

/**
 * Event context with information about the event that is currently
 * {@link org.tools4j.elara.application.EventProcessor#onEvent(Event, EventContext, InFlightState, CommandSender) processed}.
 * <p>
 * Note that the {@link #processedEvent() prcessed} event is not necessarily the {@link #mostRecentEvent() most recent}
 * event.  If not all events corresponding to sent commands have been received back, event processing can be
 * {@link org.tools4j.elara.application.EventProcessor.Ack#DEFERRED deferred} until all events have been received and
 * the applications state has been brought up-to-date.
 * <p>
 * For instance if a request event A1 is received that results in a command B1, then we wait for the event B1 to be
 * routed back to us so that it can be used to update the state.  Only then are we ready to process the next request.
 * If a request event A2 arrives before event B1, then it is <i>deferred</i> because we know that the application state
 * is not up-to-date; A2 will be processed after processing the B1 event and bringing the state up-to-date.
 * <p>
 * See also {@link org.tools4j.elara.application.EventProcessor EventProcessor} and {@link InFlightState} description
 * for more information.
 */
public interface EventContext {

    /**
     * The event that is currently processed (but not necessarily the {@link #mostRecentEvent() most recent} event).
     * @return the processed (most recent or deferred) event
     */
    Event processedEvent();

    /**
     * The most-recent event.  This is typically the same as the {@link #processedEvent() processed} unless the
     * processed event was deferred to wait for an event in response to an in-flight command.  If the processed
     * event was deferred then the returned most recent event was the waited-for event that triggered the processing of
     * the deferred event.
     * @return the most recent event, same as processed event if it was not deferred
     */
    Event mostRecentEvent();

    /**
     * True if the {@link #processedEvent() processed} event is an event from a command that we sent ourselves.
     * @return true if the event's source is equal to the {@link CommandSender#source() source} of commands sent by us
     */
    boolean isOwnEvent();

    /**
     * True if the {@link #processedEvent()} processed} event was deferred to wait for an event in response to an
     * in-flight command, and false if the processed event is also the most recent event.
     * @return true if processed event was deferred, and false if it is the most recent event
     */
    boolean isDeferredEvent();

    /** Interface extension providing default implementations */
    interface Default extends EventContext {
        @Override
        default boolean isDeferredEvent() {
            final Event processed = processedEvent();
            final Event recent = mostRecentEvent();
            if (processed == recent) {
                return false;
            }
            final Event.Id processedId = processed.id();
            final Event.Id recentId = recent.id();
            return processedId.source() != recentId.source() ||
                    processedId.sequence() != recentId.sequence();
        }
    }
}
