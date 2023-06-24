/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.state.EventProcessingState.MutableEventProcessingState;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.exception.DuplicateHandler;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.source.SourceContext;

import static java.util.Objects.requireNonNull;

public class EventProcessorHandler implements EventHandler {

    private final MutableEventProcessingState eventProcessingState;
    private final EventProcessor eventProcessor;
    private final SourceContext sourceContext;
    private final ExceptionHandler exceptionHandler;
    private final DuplicateHandler duplicateHandler;

    public EventProcessorHandler(final MutableEventProcessingState eventProcessingState,
                                 final EventProcessor eventProcessor,
                                 final SourceContext sourceContext,
                                 final ExceptionHandler exceptionHandler,
                                 final DuplicateHandler duplicateHandler) {
        this.eventProcessingState = requireNonNull(eventProcessingState);
        this.eventProcessor = requireNonNull(eventProcessor);
        this.sourceContext = requireNonNull(sourceContext);
        this.exceptionHandler = requireNonNull(exceptionHandler);
        this.duplicateHandler = requireNonNull(duplicateHandler);
    }

    @Override
    public void onEvent(final Event event) {
        if (eventProcessingState.eventApplied(event.eventSequence())) {
            skipEvent(event);
        } else {
            processEvent(event);
            eventProcessingState.applyEvent(event);
        }
    }

    private void processEvent(final Event event) {
        try {
            eventProcessor.onEvent(event, sourceContext.commandTracker(), sourceContext.commandSender());
        } catch (final Throwable t) {
            exceptionHandler.handleEventApplierException(event, t);
        }
    }

    private void skipEvent(final Event event) {
        try {
            duplicateHandler.skipEventProcessing(event);
        } catch (final Throwable t) {
            exceptionHandler.handleEventApplierException(event, t);
        }
    }

}
