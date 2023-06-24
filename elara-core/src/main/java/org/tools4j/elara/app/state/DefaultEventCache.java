package org.tools4j.elara.app.state;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.send.CommandSender;

import static java.util.Objects.requireNonNull;

public class DefaultEventCache implements EventCache {

    private int start;
    private int end;
    private int count;
    private final MutableDirectBuffer buffer;
    private final FlyweightEvent event = new FlyweightEvent();

    public DefaultEventCache(final int initialCapacity) {
        this(new ExpandableDirectByteBuffer(initialCapacity));
    }
    public DefaultEventCache(final MutableDirectBuffer buffer) {
        this.buffer = requireNonNull(buffer);
    }

    @Override
    public void add(final Event event) {
        final int length = event.writeTo(buffer, end + Integer.BYTES);
        buffer.putInt(end, length);
        end += length + Integer.BYTES;
        count++;
    }

    @Override
    public boolean poll(final EventHandler handler) {
        if (start < end) {
            final int length = buffer.getInt(start);
            event.wrapSilently(buffer, start + Integer.BYTES);
            try {
                handler.onEvent(event);
            } finally {
                event.reset();
                polled(length);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean poll(final CommandTracker commandTracker, final CommandSender sender, final EventProcessor processor) {
        if (start < end) {
            final int length = buffer.getInt(start);
            event.wrapSilently(buffer, start + Integer.BYTES);
            try {
                processor.onEvent(event, commandTracker, sender);
            } finally {
                event.reset();
                polled(length);
            }
            return true;
        }
        return false;
    }

    private void polled(final int length) {
        count--;
        start += length;
        if (start == end) {
            clear();
        }
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public void clear() {
        count = 0;
        start = 0;
        end = 0;
    }

    @Override
    public String toString() {
        return "DefaultEventCache{" +
                "count=" + count +
                "|bytes=" + (end - start) +
                "|capacity=" + buffer.capacity() +
                '}';
    }
}
