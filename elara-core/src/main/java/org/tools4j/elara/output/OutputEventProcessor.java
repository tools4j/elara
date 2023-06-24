package org.tools4j.elara.output;

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.send.CommandSender;

import static java.util.Objects.requireNonNull;

public class OutputEventProcessor implements EventProcessor {
    private final Output output;
    private final ExceptionHandler exceptionHandler;

    private OutputEventProcessor(final Output output, final ExceptionHandler exceptionHandler) {
        this.output = requireNonNull(output);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    public static EventProcessor create(final Output output, final ExceptionHandler exceptionHandler) {
        if (output == Output.NOOP) {
            return NOOP;
        }
        return new OutputEventProcessor(output, exceptionHandler);
    }

    @Override
    public void onEvent(final Event event, final CommandTracker commandTracker, final CommandSender sender) {
        try {
            final Ack ack = output.publish(event, false, 0);
            if (ack == Ack.RETRY) {
                throw new RuntimeException("Retry not supported for event processor output");
            }
        } catch (final Throwable t) {
            exceptionHandler.handleEventOutputException(event, t);
        }
    }
}
