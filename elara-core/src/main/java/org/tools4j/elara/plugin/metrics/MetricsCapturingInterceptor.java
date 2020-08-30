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
package org.tools4j.elara.plugin.metrics;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.factory.InterceptableSingletons;
import org.tools4j.elara.factory.Singletons;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.input.Receiver.ReceivingContext;
import org.tools4j.elara.log.MessageLog.Handler.Result;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.time.TimeSource;
import org.tools4j.nobark.loop.Step;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.COMMAND_POLL_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.COMMAND_PROCESSED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_PERFORMED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EVENT_APPLIED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EVENT_POLL_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EXTRA_STEP_INVOCATION_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EXTRA_STEP_PERFORMED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.INPUTS_POLL_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.INPUT_RECEIVED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.OUTPUT_POLL_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.OUTPUT_PUBLISHED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.STEP_ERROR_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.TimeMetric.APPLYING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.APPLYING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.COMMAND_APPENDING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.COMMAND_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.EVENT_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_POLLING_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.OUTPUT_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.PROCESSING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.PROCESSING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.ROUTING_END_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.ROUTING_START_TIME;
import static org.tools4j.elara.plugin.metrics.TimeMetric.Target.COMMAND;
import static org.tools4j.elara.plugin.metrics.TimeMetric.Target.EVENT;
import static org.tools4j.elara.plugin.metrics.TimeMetric.Target.OUTPUT;

public class MetricsCapturingInterceptor extends InterceptableSingletons {

    private final TimeSource timeSource;
    private final Configuration configuration;
    private final MetricsState state;
    private final TimeMetricsLogger logger;

    public MetricsCapturingInterceptor(final Singletons delegate,
                                       final TimeSource timeSource,
                                       final Configuration configuration,
                                       final MetricsState state) {
        super(delegate);
        this.timeSource = requireNonNull(timeSource);
        this.configuration = requireNonNull(configuration);
        this.state = requireNonNull(state);
        this.logger = new TimeMetricsLogger(timeSource, configuration, state);
    }

    private boolean shouldCapture(final TimeMetric metric) {
        return configuration.timeMetrics().contains(metric);
    }

    private boolean shouldCapture(final FrequencyMetric metric) {
        return configuration.frequencyMetrics().contains(metric);
    }

    private boolean shouldCaptureAnyOf(final Target target) {
        return target.anyOf(configuration.timeMetrics());
    }

    private void captureTime(final TimeMetric metric) {
        if (shouldCapture(metric)) {
            state.time(metric, timeSource.currentTime());
        }
    }

    private void captureCount(final FrequencyMetric metric) {
        if (shouldCapture(metric)) {
            state.counter(metric, 1);
        }
    }

    private Step counterStep(final FrequencyMetric invokedMetric,
                             final FrequencyMetric performedMetric,
                             final Step step) {
        requireNonNull(step);
        if (shouldCapture(STEP_ERROR_FREQUENCY)) {
            if (shouldCapture(invokedMetric) || shouldCapture(performedMetric)) {
                return () -> {
                    try {
                        final boolean result = step.perform();
                        captureCount(invokedMetric);
                        if (result) {
                            captureCount(performedMetric);
                        }
                        return result;
                    } catch (final Throwable t) {
                        captureCount(STEP_ERROR_FREQUENCY);
                        throw t;
                    }
                };
            }
            return () -> {
                try {
                    return step.perform();
                } catch (final Throwable t) {
                    captureCount(STEP_ERROR_FREQUENCY);
                    throw t;
                }
            };
        }
        if (shouldCapture(invokedMetric) || shouldCapture(performedMetric)) {
            return () -> {
                final boolean result = step.perform();
                captureCount(invokedMetric);
                if (result) {
                    captureCount(performedMetric);
                }
                return result;
            };
        }
        return step;
    }

    @Override
    public Step dutyCycleStep() {
        return counterStep(DUTY_CYCLE_FREQUENCY, DUTY_CYCLE_PERFORMED_FREQUENCY, singletons().dutyCycleStep());
    }

    @Override
    public Step sequencerStep() {
        return counterStep(INPUTS_POLL_FREQUENCY, INPUT_RECEIVED_FREQUENCY, singletons().sequencerStep());
    }

    @Override
    public Step commandPollerStep() {
        return counterStep(COMMAND_POLL_FREQUENCY, COMMAND_PROCESSED_FREQUENCY, singletons().commandPollerStep());
    }

    @Override
    public Step eventPollerStep() {
        return counterStep(EVENT_POLL_FREQUENCY, EVENT_APPLIED_FREQUENCY, singletons().eventPollerStep());
    }

    @Override
    public Step outputStep() {
        return counterStep(OUTPUT_POLL_FREQUENCY, OUTPUT_PUBLISHED_FREQUENCY, singletons().outputStep());
    }

    @Override
    public Step dutyCycleExtraStep() {
        return counterStep(EXTRA_STEP_INVOCATION_FREQUENCY, EXTRA_STEP_PERFORMED_FREQUENCY, singletons().dutyCycleExtraStep());
    }

    @Override
    public Receiver receiver() {
        final Receiver receiver = requireNonNull(singletons().receiver());
        if (shouldCapture(INPUT_POLLING_TIME) || shouldCapture(COMMAND_APPENDING_TIME)) {
            return timedReceiver(receiver);
        }
        return receiver;
    }

    @Override
    public CommandHandler commandHandler() {
        final CommandHandler commandHandler = requireNonNull(singletons().commandHandler());
        if (shouldCaptureAnyOf(COMMAND)) {//includes COMMAND_POLLING_TIME and PROCESSING_END_TIME
            return command -> {
                captureTime(COMMAND_POLLING_TIME);
                final Result result = commandHandler.onCommand(command);
                captureTime(PROCESSING_END_TIME);
                logger.logMetrics(command);
                return result;
            };
        }
        return commandHandler;
    }

    @Override
    public CommandProcessor commandProcessor() {
        final CommandProcessor commandProcessor = requireNonNull(singletons().commandProcessor());
        if (shouldCapture(PROCESSING_START_TIME) || shouldCapture(ROUTING_START_TIME) || shouldCapture(ROUTING_END_TIME)) {
            return (command, router) -> {
                final EventRouter eventRouter = shouldCapture(ROUTING_START_TIME) || shouldCapture(ROUTING_END_TIME) ?
                        timedEventRouter(router) : router;
                captureTime(PROCESSING_START_TIME);
                commandProcessor.onCommand(command, eventRouter);
                //PROCESSING_END_TIME is measured in CommandHandler
            };
        }
        return commandProcessor;
    }

    @Override
    public EventApplier eventApplier() {
        final EventApplier eventApplier = requireNonNull(singletons().eventApplier());
        if (shouldCaptureAnyOf(EVENT)) {//includes APPLYING_START_TIME and APPLYING_END_TIME
            return event -> {
                captureTime(APPLYING_START_TIME);
                eventApplier.onEvent(event);
                captureTime(APPLYING_END_TIME);
                logger.logMetrics(EVENT, event);
            };
        }
        return eventApplier;
    }

    @Override
    public EventHandler eventHandler() {
        final EventHandler eventHandler = requireNonNull(singletons().eventHandler());
        if (shouldCapture(EVENT_POLLING_TIME)) {
            return event -> {
                captureTime(EVENT_POLLING_TIME);
                eventHandler.onEvent(event);
            };
        }
        return eventHandler;
    }

    @Override
    public OutputHandler outputHandler() {
        final OutputHandler outputHandler = requireNonNull(singletons().outputHandler());
        if (shouldCapture(OUTPUT_POLLING_TIME)) {
            return (event, replay, retry) -> {
                captureTime(OUTPUT_POLLING_TIME);
                return outputHandler.publish(event, replay, retry);
            };
        }
        return outputHandler;
    }

    @Override
    public Output output() {
        final Output output = requireNonNull(singletons().output());
        if (shouldCaptureAnyOf(OUTPUT)) {//includes OUTPUT_START_TIME and OUTPUT_END_TIME
            return (event, replay, retry, loopback) -> {
                captureTime(OUTPUT_START_TIME);
                final Ack ack = output.publish(event, replay, retry, loopback);
                captureTime(OUTPUT_END_TIME);
                logger.logMetrics(OUTPUT, event);
                return ack;
            };
        }
        return output;
    }

    private EventRouter timedEventRouter(final EventRouter router) {
        requireNonNull(router);
        return new EventRouter() {
            final TimedRoutingContext context = new TimedRoutingContext();
            @Override
            public RoutingContext routingEvent() {
                return context.init(router.routingEvent());
            }

            @Override
            public RoutingContext routingEvent(final int type) {
                return context.init(router.routingEvent(type));
            }

            @Override
            public void routeEvent(final DirectBuffer buffer, final int offset, final int length) {
                captureTime(ROUTING_START_TIME);
                router.routeEvent(buffer, offset, length);
                captureTime(ROUTING_END_TIME);
            }

            @Override
            public void routeEvent(final int type, final DirectBuffer buffer, final int offset, final int length) {
                captureTime(ROUTING_START_TIME);
                router.routeEvent(type, buffer, offset, length);
                captureTime(ROUTING_END_TIME);
            }

            @Override
            public void routeEventWithoutPayload(final int type) {
                captureTime(ROUTING_START_TIME);
                router.routeEventWithoutPayload(type);
                captureTime(ROUTING_END_TIME);
            }

            @Override
            public short nextEventIndex() {
                return router.nextEventIndex();
            }

            @Override
            public boolean skipCommand() {
                return router.skipCommand();
            }

            @Override
            public boolean isSkipped() {
                return router.isSkipped();
            }
        };
    }

    private final class TimedRoutingContext implements RoutingContext {
        RoutingContext context;
        TimedRoutingContext init(final RoutingContext context) {
            this.context = requireNonNull(context);
            return this;
        }

        RoutingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Routing context is closed");
        }

        @Override
        public int index() {
            return unclosedContext().index();
        }

        @Override
        public MutableDirectBuffer buffer() {
            return unclosedContext().buffer();
        }

        @Override
        public void route(final int length) {
            unclosedContext().route(length);
            context = null;
            captureTime(ROUTING_END_TIME);
        }

        @Override
        public void abort() {
            if (context != null) {
                context.abort();
                context = null;
            }
        }

        @Override
        public boolean isClosed() {
            return context == null || context.isClosed();
        }
    }

    private Receiver timedReceiver(final Receiver receiver) {
        requireNonNull(receiver);
        return new Receiver() {
            final TimedReceivingContext context = shouldCapture(COMMAND_APPENDING_TIME) ?
                    new TimedReceivingContext() : null;
            ReceivingContext receivingContext(final ReceivingContext receivingContext) {
                return context != null ? context.init(receivingContext) : receivingContext;
            }

            @Override
            public ReceivingContext receivingMessage(final int source, final long sequence) {
                captureTime(INPUT_POLLING_TIME);
                return receivingContext(receiver.receivingMessage(source, sequence));
            }

            @Override
            public ReceivingContext receivingMessage(final int source, final long sequence, final int type) {
                captureTime(INPUT_POLLING_TIME);
                return receivingContext(receiver.receivingMessage(source, sequence, type));
            }

            @Override
            public void receiveMessage(final int source, final long sequence, final DirectBuffer buffer, final int offset, final int length) {
                captureTime(INPUT_POLLING_TIME);
                receiver.receiveMessage(source, sequence, buffer, offset, length);
                captureTime(COMMAND_APPENDING_TIME);
            }

            @Override
            public void receiveMessage(final int source, final long sequence, final int type, final DirectBuffer buffer, final int offset, final int length) {
                captureTime(INPUT_POLLING_TIME);
                receiver.receiveMessage(source, sequence, type, buffer, offset, length);
                captureTime(COMMAND_APPENDING_TIME);
            }

            @Override
            public void receiveMessageWithoutPayload(final int source, final long sequence, final int type) {
                captureTime(INPUT_POLLING_TIME);
                receiver.receiveMessageWithoutPayload(source, sequence, type);
                captureTime(COMMAND_APPENDING_TIME);
            }
        };
    }

    private final class TimedReceivingContext implements ReceivingContext {
        ReceivingContext context;
        TimedReceivingContext init(final ReceivingContext context) {
            this.context = requireNonNull(context);
            return this;
        }

        ReceivingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Receiving context is closed");
        }

        @Override
        public MutableDirectBuffer buffer() {
            return unclosedContext().buffer();
        }

        @Override
        public void receive(final int length) {
            unclosedContext().receive(length);
            context = null;
            captureTime(COMMAND_APPENDING_TIME);
        }

        @Override
        public void abort() {
            if (context != null) {
                context.abort();
                context = null;
            }
        }

        @Override
        public boolean isClosed() {
            return context == null || context.isClosed();
        }
    }
}
