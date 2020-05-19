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

import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.base.BaseState;

import static java.util.Objects.requireNonNull;

/**
 * Event handler that appends and outputs outputs events.
 */
public class EventHandler implements EventApplier {

    private final BaseState.Mutable baseState;
    private final CommandLoopback commandLoopback;
    private final Output output;
    private final EventApplier eventApplier;
    private final ExceptionHandler exceptionHandler;
    private final DuplicateHandler duplicateHandler;

    public EventHandler(final BaseState.Mutable baseState,
                        final CommandLoopback commandLoopback,
                        final Output output,
                        final EventApplier eventApplier,
                        final ExceptionHandler exceptionHandler,
                        final DuplicateHandler duplicateHandler) {
        this.baseState = requireNonNull(baseState);
        this.commandLoopback = requireNonNull(commandLoopback);
        this.output = requireNonNull(output);
        this.eventApplier = requireNonNull(eventApplier);
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.duplicateHandler = requireNonNull(duplicateHandler);
    }

    @Override
    public void onEvent(final Event event) {
        if (baseState.eventApplied(event.id())) {
            skipEvent(event);
        } else {
            final boolean replay = !baseState.allEventsPolled();
            applyEvent(event);
            updateBaseState(event);
            publishEvent(event, replay);
        }
    }

    private void publishEvent(final Event event, final boolean replay) {
        try {
            output.publish(event, replay, commandLoopback);
        } catch (final Throwable t) {
            exceptionHandler.handleEventOutputException(event, t);
        }
    }

    private void applyEvent(final Event event) {
        try {
            eventApplier.onEvent(event);
        } catch (final Throwable t) {
            exceptionHandler.handleEventApplierException(event, t);
        }
    }

    private void updateBaseState(final Event event) {
        baseState.lastAppliedEvent(event);//TODO should we only call this if event.id.index == 0 ?
        if (event.flags().isCommit()) {
            baseState.allEventsAppliedFor(event.id().commandId());
        }
    }

    private void skipEvent(final Event event) {
        try {
            duplicateHandler.skipEventApplying(event);
        } catch (final Throwable t) {
            exceptionHandler.handleEventApplierException(event, t);
        }
    }

}
