package org.tools4j.elara.app.state;

import org.tools4j.elara.time.TimeSource;

/**
 * Transient state associated with events known to exist but that have not been received yet, hence this state is
 * non-deterministic.  Transient event state can be used to determine if an application has received all known events
 * and is ready to start sending commands.
 * <p>
 * Note however that transient state should not be used in the decision-making logic of the application, otherwise its
 * state will not be deterministic and cannot be reproduced through event replay.
 *
 * @see TransientState
 */
public interface TransientEventState extends TransientState {
    /**
     * Returns the number of events known to exist, but not all may have been received yet.
     * @return the number of events known to exist
     */
    long eventsKnownToExist();

    /**
     * Returns the event sequence of the most recent event known to exist, or
     * {@link BaseState#NIL_SEQUENCE NIL_SEQUENCE} if no event is known yet.
     *
     * @return the event sequence of the last event known
     */
    long eventSequenceOfLastKnownEvent();

    /**
     * Returns the event time of the most recent event known to exist, or {@link TimeSource#MIN_VALUE} if no event
     * is known yet.
     *
     * @return the event time of the last event known
     */
    long eventTimeOfLastKnownEvent();

    /**
     * Returns the receiving time of the most recently received event, or {@link TimeSource#MIN_VALUE} if no event
     * is known yet.
     *
     * @return the receiving time of the last event known
     */
    long receivingTimeOfLastReceivedEvent();
}
