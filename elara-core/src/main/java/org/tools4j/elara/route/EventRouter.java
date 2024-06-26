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
package org.tools4j.elara.route;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.flyweight.PayloadType;

/**
 * Facilitates routing of events when
 * {@link CommandProcessor#onCommand(Command, EventRouter) processing} commands.
 * <p>
 * Event routing can be done in two ways: already coded events can be routed via one of the
 * {@link #routeEvent(DirectBuffer, int, int) routeEvent(..)} methods.  Alternatively the event can be encoded into the
 * event store buffer directly as follows:
 * <pre>
 *     try (RoutingContext context = routingEvent()) {
 *         int length = context.buffer().putStringAscii(0, "Hello world");
 *         context.route(length);
 *     }
 * </pre>
 * Note that {@code RoutingContext} implements {@link AutoCloseable} and if event routing is performed inside a
 * try-resource block as in the example above then routing will be {@link RoutingContext#abort() aborted} automatically
 * if {@link RoutingContext#route(int) route(..)} is not called for instance due to an exception.
 */
public interface EventRouter {
    /**
     * Starts routing of an {@link PayloadType#DEFAULT APPLICATION} event and returns the routing context with the
     * buffer for event encoding.  Encoding and routing is completed with {@link RoutingContext#route(int) route(..)}
     * and is recommended to be performed inside a try-resource block; see {@link EventRouter class documentation} for
     * an example.
     *
     * @return the context for event encoding and routing
     */
    RoutingContext routingEvent();
    /**
     * Starts routing of an event of the given {@code type} returning the routing context with the buffer for event
     * encoding.  Encoding and routing is completed with {@link RoutingContext#route(int) route(..)} and is recommended
     * to be performed inside a try-resource block; see {@link EventRouter class documentation} for an example.
     *
     * @param type the event type, typically non-negative for application events (plugins use negative types)
     * @return the context for event encoding and routing
     */
    RoutingContext routingEvent(int type);

    /***
     * Routes an {@link PayloadType#DEFAULT APPLICATION} event already encoded in the given buffer.
     *
     * @param buffer    the buffer containing the event data
     * @param offset    offset where the event data starts in {@code buffer}
     * @param length    the length of the event data in bytes
     * @throws IllegalStateException if this command has been {@link #isSkipped() skipped}
     */
    void routeEvent(DirectBuffer buffer, int offset, int length);

    /***
     * Routes an already encoded event of the specified event {@code payloadType}.
     *
     * @param payloadType   the payload type, typically non-negative for application events (plugins use negative types)
     * @param buffer        the buffer containing the event data
     * @param offset        offset where the event data starts in {@code buffer}
     * @param length        the length of the event data in bytes
     * @throws IllegalStateException if this command has been {@link #isSkipped() skipped}
     */
    void routeEvent(int payloadType, DirectBuffer buffer, int offset, int length);

    /***
     * Routes an event that carries the same payload data as the {@link #command() command};  the {@link Command#payloadType() command type} is
     * used as event type.
     *
     * @throws IllegalStateException if this command has been {@link #isSkipped() skipped}
     */
    void routeEventWithCommandPayload();

    /***
     * Routes an event of the specified event {@code payloadType} that carries the same payload data as the
     * {@link #command() command}.
     *
     * @param payloadType the payload type, typically non-negative for application events (plugins use negative types)
     * @throws IllegalStateException if this command has been {@link #isSkipped() skipped}
     */
    void routeEventWithCommandPayload(int payloadType);

    /***
     * Routes an event of the specified event {@code payloadType} that carries no payload data.
     *
     * @param payloadType the payload type, typically non-negative for application events (plugins use negative types)
     * @throws IllegalStateException if this command has been {@link #isSkipped() skipped}
     */
    void routeEventWithoutPayload(int payloadType);

    /**
     * Returns the event sequence of the next event to be routed.  If routing has started via {@link #routingEvent()}
     * then the sequence refers to the event currently being encoded.
     * <p>
     * Event sequence is a monotonically increasing sequence number for all events ever applied to the application.
     *
     * @return index of the next event to be routed.
     */
    long nextEventSequence();

    /**
     * Returns the zero based index of the next event to be routed.  If routing has started via {@link #routingEvent()}
     * then the index refers to the event currently being encoded.
     * <p>
     * Event index is a zero based index for all events routed from the same command.  For every new command, the event
     * index will be reset to zero.  If only one event is routed for a command, its index will be zero.
     *
     * @return index of the next event to be routed.
     */
    int nextEventIndex();

    /**
     * Skip the current command if possible.  Skipping is only possible when no events have been routed yet.  Routing
     * events after the command was skipped will result in an exception.  Calling skip repeatedly has no further effect
     * and returns the same result as for the first invocation.
     * <p>
     * Skipping a command results in no events and will therefore leave the application state in exactly the same as
     * before the command.  Note that if a command is not skipped, an event will be applied implicitly even if the
     * application itself did not route an event. This means that all non-skipped commands will be marked as
     * 'processed'.
     *
     * @return true if the command was skipped, and false if events have been routed already
     * @see #isSkipped()
     */
    boolean skipCommand();

    /**
     * If true the current command was skipped without routing any events.  Routing events after the command was skipped
     * will result in an exception.
     * <p>
     * The application state after processing the current command will be exactly the same as before the command. Note
     * that the command will not be marked as processed and is normally also not replayed.  Replaying of the command may
     * occur however if the application is restarted or leadership is passed to another node and commands are replayed
     * from the command store.  However, replaying occurs only if no subsequent command from the same source has been
     * processed and not skipped.
     *
     * @return true if this command is skipped
     * @see #skipCommand()
     */
    boolean isSkipped();

    /**
     * Returns the command currently associated with this even router.  Routed events are the result of processing this
     * command and hence are associated with the command.
     *
     * @return the command currently associated with this event router
     * @throws IllegalStateException if no command is currently associated with this event router
     */
    Command command();

    /**
     * Context object returned by {@link #routingEvent()} allowing for zero copy encoding of events directly into the
     * event store buffer.  Routing contexts are typically used inside a try-resource block; see {@code EventRouter}
     * {@link EventRouter documentation} for usage example.
     */
    interface RoutingContext extends AutoCloseable {
        /** @return index of the event currently being routed */
        int index();

        /**
         * Returns the buffer to encode the event directly into the event store.
         *
         * @return the buffer for coding of event data directly into the event store
         *
         * @throws IllegalStateException if this routing context has already been {@link #isClosed() closed}
         */
        MutableDirectBuffer buffer();

        /**
         * Completes event encoding and routes the event; the event will be applied to the application context but it
         * cannot be polled from the event store yet (starting the next event or completion of command will make the event
         * available for polling).
         *
         * @param length the encoding length for the routed event
         * @throws IllegalArgumentException if length is negative
         * @throws IllegalStateException if this routing context has already been {@link #isClosed() closed}
         */
        void route(int length);

        /**
         * Aborts routing of the event -- identical to {@link #close()}; ignored if the routing context is already
         * {@link #isClosed() closed}.
         */
        void abort();

        /**
         * Returns true if this routing context has already been closed through either of {@link #route(int)},
         * {@link #abort()} or {@link #close()}.
         *
         * @return true if this routing context is closed (event routed or routing aborted)
         */
        boolean isClosed();

        /**
         * Aborts routing of the event -- identical to {@link #abort()}; ignored if the routing context is already
         * {@link #isClosed() closed}.
         */
        @Override
        default void close() {
            if (!isClosed()) {
                abort();
            }
        }
    }

    /**
     * Provides default methods for {@link EventRouter}.
     */
    interface Default extends EventRouter {
        @Override
        default RoutingContext routingEvent() {
            return routingEvent(PayloadType.DEFAULT);
        }

        @Override
        default void routeEvent(final DirectBuffer buffer, final int offset, final int length) {
            routeEvent(PayloadType.DEFAULT, buffer, offset, length);
        }

        @Override
        default void routeEvent(final int payloadType, final DirectBuffer buffer, final int offset, final int length) {
            try (final RoutingContext context = routingEvent(payloadType)) {
                context.buffer().putBytes(0, buffer, offset, length);
                context.route(length);
            }
        }

        @Override
        default void routeEventWithCommandPayload() {
            final Command command = command();
            final int commandType = command().payloadType();
            final DirectBuffer payload = command.payload();
            routeEvent(commandType, payload, 0, payload.capacity());
        }

        @Override
        default void routeEventWithCommandPayload(final int payloadType) {
            final DirectBuffer payload = command().payload();
            routeEvent(payloadType, payload, 0, payload.capacity());
        }

        @Override
        default void routeEventWithoutPayload(final int payloadType) {
            try (final RoutingContext context = routingEvent(payloadType)) {
                context.route(0);
            }
        }
    }
}
