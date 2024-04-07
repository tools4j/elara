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
package org.tools4j.elara.app.handler;

import org.tools4j.elara.event.Event;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;

/**
 * Event processor called to process events in a feedback app.  Note that own commands from previous processing
 * invocations can still be {@link CommandContext#hasInFlightCommand() in-flight}.  This means the current state of this
 * application may not be up-to-date yet.
 * <p>
 * To understand the concept of in-flight commands, it is important to understand that application state should only be
 * modified when events are received and applied, otherwise determinism cannot be guaranteed.  For instance if we update
 * the state already when sending a command, and the command is subsequently lost in transition, then subsequent
 * decisions are made on the basis of a corrupted application state.  The application state is corrupted because it can
 * no longer be reconstructed from events.
 */
@FunctionalInterface
public interface EventProcessor {
    /** Event processor that ignores all events */
    EventProcessor NOOP = (event, state, sender) -> {};

    /**
     * Invoked to process the given event.
     *
     * @param event             the processed event
     * @param commandContext    command context with command sources and tracking information for sent commands, for
     *                          instance to provide insights about {@link CommandContext#hasInFlightCommand() in-flight}
     *                          commands
     * @param sender            handler to encode and send commands back to the sequencer
     */
    void onEvent(Event event, CommandContext commandContext, CommandSender sender);
}
