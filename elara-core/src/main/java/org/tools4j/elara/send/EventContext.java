package org.tools4j.elara.send;

import org.tools4j.elara.event.Event;

/**
 * Event context with information about the event that is currently {@link #processedEvent() processed}.
 * <p>
 * Note that the {@link #processedEvent() prcessed} event is not necessarily the {@link #mostRecentEvent() most recent}
 * event.  If not configured otherwise, processing of an event is only performed if all own events have been received.
 * <p>
 * For instance if a request event A1 is received that results in a command B1, then we wait for the event B1 to be
 * routed back to us so that it can be used to update the state.  Only then are we ready to process the next request.
 * If a request event A2 arrives before event B1, then it is <i>parked</i> because we know that the application state
 * is not up-to-date.  A2 is <i>un-parked</i> and processed when the B1 event arrives.
 */
public interface EventContext {

    /**
     * The event that is currently processed (but not necessarily the {@link #mostRecentEvent() most recent} event).
     * @return the processed (most recent or un-parked) event
     */
    Event processedEvent();

    /**
     * The most-recent event.  This is typically the same as the {@link #processedEvent() processed} unless the
     * processed event had to be parked to wait for an event in response to an in-flight command.  If the processed
     * event was parked then the returned most recent event was the waited-for event that triggered the un-parking of
     * the processed event.
     * @return the most recent event, same as processed event unless un-parking was triggered by the returned event
     */
    Event mostRecentEvent();

    /**
     * Event time of the {@link #mostRecentEvent() most recent} event.
     * @return time of the most recent event
     */
    long mostRecentEventTime();

    /**
     * True if the {@link #processedEvent()} processed} event had to be parked to wait for an event in response to an
     * in-flight command, and false if the processed event is also the most recent event.
     * @return
     */
    boolean processedEventWasParked();

    interface Default extends EventContext {
        @Override
        default boolean processedEventWasParked() {
            return processedEvent() != mostRecentEvent();
        }

        @Override
        default long mostRecentEventTime() {
            return mostRecentEvent().time();
        }
    }
}
