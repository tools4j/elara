/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.handler;

import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState;

import static java.util.Objects.requireNonNull;

public class EventHandler implements MessageLog.Handler<Event> {

    private final BaseState.Mutable serverState;
    private final CommandLoopback commandLoopback;
    private final Output output;
    private final EventApplier eventApplier;
    private final ExceptionHandler exceptionHandler;

    public EventHandler(final BaseState.Mutable serverState,
                        final CommandLoopback commandLoopback,
                        final Output output,
                        final EventApplier eventApplier,
                        final ExceptionHandler exceptionHandler) {
        this.serverState = requireNonNull(serverState);
        this.commandLoopback = requireNonNull(commandLoopback);
        this.output = requireNonNull(output);
        this.eventApplier = requireNonNull(eventApplier);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public void onMessage(final Event event) {
        final Command.Id commandId = event.id().commandId();
        final long lastAppliedForInput = serverState.lastCommandAllEventsApplied(commandId.input());
        if (lastAppliedForInput < commandId.sequence()) {
            publishEvent(event);
            applyEvent(event);
            if (event.type() == EventType.COMMIT) {
                serverState.allEventsAppliedFor(event.id().commandId());
            }
        }
    }

    private void publishEvent(final Event event) {
        try {
            output.publish(event);
        } catch (final Throwable t) {
            exceptionHandler.handleEventOutputException(event, t);
        }
    }

    private void applyEvent(final Event event) {
        try {
            eventApplier.onEvent(event, commandLoopback);
        } catch (final Throwable t) {
            exceptionHandler.handleEventApplierException(event, t);
        }
    }
}
