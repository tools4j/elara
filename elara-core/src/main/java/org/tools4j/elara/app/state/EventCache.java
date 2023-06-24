package org.tools4j.elara.app.state;

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.send.CommandSender;

/**
 * A cache to store events for later processing, for instance due to in-flight commands.  The cache is organized as a
 * FIFO queue: events are added to the end and polled from the start of the queue.
 * 
 * @see CommandTracker#hasInFlightCommand()
 * @see EventProcessor#onEvent(Event, CommandTracker, CommandSender)
 */
public interface EventCache {
    int count();
    boolean isEmpty();
    void add(Event event);

    boolean poll(EventHandler handler);
    boolean poll(CommandTracker commandTracker, CommandSender sender, EventProcessor processor);

    void clear();

    static EventCache create(final int initialCapacity) {
        return new DefaultEventCache(initialCapacity);
    }
}
