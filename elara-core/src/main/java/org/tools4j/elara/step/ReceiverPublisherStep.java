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
package org.tools4j.elara.step;

import org.agrona.DirectBuffer;
import org.tools4j.elara.exception.ExceptionHandler;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.stream.MessageReceiver;

import static java.util.Objects.requireNonNull;

/**
 * Agent to poll and publish events from a message stream via {@link MessageReceiver}.
 */
public class ReceiverPublisherStep implements AgentStep {

    private static final int MAX_RETRIES = 3;

    private final OutputHandler handler;
    private final MessageReceiver receiver;
    private final ExceptionHandler exceptionHandler;
    private final MessageReceiver.Handler receiverHandler = this::onMessage;
    private final FlyweightEvent flyweightEvent = new FlyweightEvent();

    private Exception outputFailed;

    public ReceiverPublisherStep(final OutputHandler handler,
                                 final MessageReceiver receiver,
                                 final ExceptionHandler exceptionHandler) {
        this.handler = requireNonNull(handler);
        this.receiver = requireNonNull(receiver);
        this.exceptionHandler = requireNonNull(exceptionHandler);
    }

    @Override
    public int doWork() {
        return receiver.poll(receiverHandler);
    }

    private void onMessage(final DirectBuffer message) {
        flyweightEvent.init(message, 0);
        try {
            for (int retry = 0; retry < MAX_RETRIES; retry++) {
                final Ack ack = handler.publish(flyweightEvent, false, retry);
                if (Ack.RETRY != ack) {
                    return;
                }
            }
            if (outputFailed == null) {
                outputFailed = new IllegalStateException("Event output failed after " + MAX_RETRIES + " attempts");
            }
            exceptionHandler.handleEventOutputException(flyweightEvent, outputFailed);
        } finally {
            flyweightEvent.reset();
        }
    }

}
