/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.app.factory.AgentStepFactory;
import org.tools4j.elara.app.factory.AppFactory;
import org.tools4j.elara.app.factory.ApplierFactory;
import org.tools4j.elara.app.factory.CommandPollerFactory;
import org.tools4j.elara.app.factory.InOutFactory;
import org.tools4j.elara.app.factory.Interceptor;
import org.tools4j.elara.app.factory.ProcessorFactory;
import org.tools4j.elara.app.factory.PublisherFactory;
import org.tools4j.elara.app.factory.SequencerFactory;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.CommandType;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.output.Output.Ack;
import org.tools4j.elara.plugin.base.EventIdApplier;
import org.tools4j.elara.plugin.metrics.TimeMetric.Target;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.CommandSender.SendingContext;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.store.MessageStore.Handler.Result;
import org.tools4j.elara.stream.SendingResult;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.COMMAND_POLL_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.COMMAND_PROCESSED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.DUTY_CYCLE_PERFORMED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EVENT_APPLIED_FREQUENCY;
import static org.tools4j.elara.plugin.metrics.FrequencyMetric.EVENT_POLLED_FREQUENCY;
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
import static org.tools4j.elara.plugin.metrics.TimeMetric.INPUT_SENDING_TIME;
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

public class MetricsCapturingInterceptor implements Interceptor {

    private final TimeSource timeSource;
    private final MetricsConfig configuration;
    private final MetricsState state;
    private final TimeMetricsStoreWriter timeMetricsWriter;

    public MetricsCapturingInterceptor(final TimeSource timeSource,
                                       final MetricsConfig configuration,
                                       final MetricsState state) {
        this.timeSource = requireNonNull(timeSource);
        this.configuration = requireNonNull(configuration);
        this.state = requireNonNull(state);
        this.timeMetricsWriter = configuration.timeMetrics().isEmpty() ? null : new TimeMetricsStoreWriter(timeSource, configuration, state);
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

    private void captureInputSendingTime(final int source, final long sequence, final int type,
                                         final DirectBuffer buffer, final int offset, final int length) {
        if (shouldCapture(INPUT_SENDING_TIME)) {
            final long sendingTime = configuration.inputSendingTimeExtractor().sendingTime(source, sequence, type,
                    buffer, offset, length);
            state.time(INPUT_SENDING_TIME, sendingTime);
        }
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

    @SuppressWarnings("SameParameterValue")
    private Agent counterAgent(final FrequencyMetric invokedMetric,
                               final FrequencyMetric performedMetric,
                               final Agent agent) {
        requireNonNull(invokedMetric);
        requireNonNull(performedMetric);
        requireNonNull(agent);
        if (shouldCapture(invokedMetric) || shouldCapture(performedMetric)) {
            return new Agent() {
                @Override
                public void onStart() {
                    agent.onStart();
                }

                @Override
                public int doWork() throws Exception {
                    final int workDone = agent.doWork();
                    captureCount(invokedMetric);
                    if (workDone > 0) {
                        captureCount(performedMetric);
                    }
                    return workDone;
                }

                @Override
                public void onClose() {
                    agent.onClose();
                }

                @Override
                public String roleName() {
                    return agent.roleName();
                }
            };
        }
        return agent;
    }

    private AgentStep counterStep(final FrequencyMetric invokedMetric,
                                  final FrequencyMetric performedMetric,
                                  final AgentStep step) {
        requireNonNull(invokedMetric);
        //nullable: performedMetric
        requireNonNull(step);
        if (shouldCapture(STEP_ERROR_FREQUENCY)) {
            if (shouldCapture(invokedMetric) || shouldCapture(performedMetric)) {
                return () -> {
                    try {
                        final int workDone = step.doWork();
                        captureCount(invokedMetric);
                        if (workDone > 0 && performedMetric != null) {
                            captureCount(performedMetric);
                        }
                        return workDone;
                    } catch (final Throwable t) {
                        captureCount(STEP_ERROR_FREQUENCY);
                        throw t;
                    }
                };
            }
            return () -> {
                try {
                    return step.doWork();
                } catch (final Throwable t) {
                    captureCount(STEP_ERROR_FREQUENCY);
                    throw t;
                }
            };
        }
        if (shouldCapture(invokedMetric) || shouldCapture(performedMetric)) {
            return () -> {
                final int workDone = step.doWork();
                captureCount(invokedMetric);
                if (workDone > 0) {
                    captureCount(performedMetric);
                }
                return workDone;
            };
        }
        return step;
    }

    @Override
    public AppFactory appFactory(final Supplier<? extends AppFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(DUTY_CYCLE_FREQUENCY) || shouldCapture(DUTY_CYCLE_PERFORMED_FREQUENCY)) {
            //noinspection Convert2Lambda
            return new AppFactory() {
                @Override
                public Agent agent() {
                    return counterAgent(DUTY_CYCLE_FREQUENCY, DUTY_CYCLE_PERFORMED_FREQUENCY, singletons.get().agent());
                }
            };
        }
        return null;
    }

    @Override
    public AgentStepFactory agentStepFactory(final Supplier<? extends AgentStepFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(STEP_ERROR_FREQUENCY) || shouldCapture(EXTRA_STEP_INVOCATION_FREQUENCY) || shouldCapture(EXTRA_STEP_PERFORMED_FREQUENCY)) {
            return new AgentStepFactory() {
                @Override
                public Runnable initStep() {
                    return singletons.get().initStep();
                }

                @Override
                public AgentStep extraStepAlwaysWhenEventsApplied() {
                    return counterStep(EXTRA_STEP_INVOCATION_FREQUENCY, EXTRA_STEP_PERFORMED_FREQUENCY, singletons.get().extraStepAlwaysWhenEventsApplied());
                }

                @Override
                public AgentStep extraStepAlways() {
                    return counterStep(EXTRA_STEP_INVOCATION_FREQUENCY, EXTRA_STEP_PERFORMED_FREQUENCY, singletons.get().extraStepAlways());
                }
            };
        }
        return null;
    }

    @Override
    public SequencerFactory sequencerFactory(final Supplier<? extends SequencerFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(STEP_ERROR_FREQUENCY) ||
                shouldCapture(INPUTS_POLL_FREQUENCY) || shouldCapture(INPUT_RECEIVED_FREQUENCY) ||
                shouldCapture(INPUT_SENDING_TIME) || shouldCapture(INPUT_POLLING_TIME) || shouldCapture(COMMAND_APPENDING_TIME)
        ) {
            return new SequencerFactory() {
                @Override
                public SenderSupplier senderSupplier() {
                    final SenderSupplier senderSupplier = singletons.get().senderSupplier();
                    if (shouldCapture(INPUT_SENDING_TIME) || shouldCapture(INPUT_POLLING_TIME) || shouldCapture(COMMAND_APPENDING_TIME)) {
                        return timedSenderSupplier(senderSupplier);
                    }
                    return senderSupplier;
                }

                @Override
                public AgentStep sequencerStep() {
                    return counterStep(INPUTS_POLL_FREQUENCY, INPUT_RECEIVED_FREQUENCY, singletons.get().sequencerStep());
                }
            };
        }
        return null;
    }

    @Override
    public CommandPollerFactory commandPollerFactory(final Supplier<? extends CommandPollerFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(STEP_ERROR_FREQUENCY) || shouldCapture(COMMAND_POLL_FREQUENCY)) {
            //noinspection Convert2Lambda
            return new CommandPollerFactory() {
                @Override
                public AgentStep commandPollerStep() {
                    return counterStep(COMMAND_POLL_FREQUENCY, null, singletons.get().commandPollerStep());
                }
            };
        }
        return null;
    }

    @Override
    public ProcessorFactory processorFactory(final Supplier<? extends ProcessorFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(COMMAND_PROCESSED_FREQUENCY) ||
                shouldCaptureAnyOf(COMMAND) || //includes COMMAND_POLLING_TIME and PROCESSING_END_TIME shouldCapture(PROCESSING_START_TIME) || shouldCapture(ROUTING_START_TIME) || shouldCapture(ROUTING_END_TIME)) {
                shouldCapture(PROCESSING_START_TIME) || shouldCapture(ROUTING_START_TIME) || shouldCapture(ROUTING_END_TIME)
        ) {
            return new ProcessorFactory() {
                @Override
                public CommandProcessor commandProcessor() {
                    final CommandProcessor commandProcessor = singletons.get().commandProcessor();
                    if (shouldCapture(COMMAND_PROCESSED_FREQUENCY) ||
                            shouldCapture(PROCESSING_START_TIME) ||
                            shouldCapture(ROUTING_START_TIME) || shouldCapture(ROUTING_END_TIME)
                    ) {
                        final TimedEventRouter timedEventRouter = shouldCapture(ROUTING_START_TIME) || shouldCapture(ROUTING_END_TIME) ?
                                new TimedEventRouter() : null;
                        return (command, router) -> {
                            final EventRouter eventRouter = timedEventRouter != null ? timedEventRouter.init(router) : router;
                            captureTime(PROCESSING_START_TIME);
                            commandProcessor.onCommand(command, eventRouter);
                            captureCount(COMMAND_PROCESSED_FREQUENCY);
                            //PROCESSING_END_TIME is measured in CommandHandler
                        };
                    }
                    return commandProcessor;
                }

                @Override
                public CommandHandler commandHandler() {
                    final CommandHandler commandHandler = singletons.get().commandHandler();
                    if (shouldCaptureAnyOf(COMMAND)) {//includes COMMAND_POLLING_TIME and PROCESSING_END_TIME
                        return command -> {
                            captureTime(COMMAND_POLLING_TIME);
                            final Result result = commandHandler.onCommand(command);
                            captureTime(PROCESSING_END_TIME);
                            timeMetricsWriter.writeMetrics(command);
                            return result;
                        };
                    }
                    return commandHandler;
                }
            };
        }
        return null;
    }

    @Override
    public ApplierFactory applierFactory(final Supplier<? extends ApplierFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(STEP_ERROR_FREQUENCY) ||
                shouldCapture(EVENT_POLL_FREQUENCY) || shouldCapture(EVENT_POLLED_FREQUENCY) ||
                shouldCapture(EVENT_APPLIED_FREQUENCY) || shouldCaptureAnyOf(EVENT) || //includes APPLYING_START_TIME and APPLYING_END_TIME
                shouldCapture(EVENT_POLLING_TIME)
        ) {
            return new ApplierFactory() {
                public EventApplier eventApplier() {
                    final EventApplier eventApplier = singletons.get().eventApplier();
                    if (shouldCapture(EVENT_APPLIED_FREQUENCY) || shouldCaptureAnyOf(EVENT)) {//includes APPLYING_START_TIME and APPLYING_END_TIME
                        if (eventApplier instanceof EventIdApplier) {
                            final EventIdApplier eventIdApplier = (EventIdApplier)eventApplier;
                            return (EventIdApplier)(source, sequence, index) -> {
                                captureTime(APPLYING_START_TIME);
                                eventIdApplier.onEventId(source, sequence, index);
                                captureTime(APPLYING_END_TIME);
                                captureCount(EVENT_APPLIED_FREQUENCY);
                                if (timeMetricsWriter != null) {
                                    timeMetricsWriter.writeMetrics(EVENT, source, sequence, (short)index);
                                }
                            };
                        }
                        return event -> {
                            captureTime(APPLYING_START_TIME);
                            eventApplier.onEvent(event);
                            captureTime(APPLYING_END_TIME);
                            captureCount(EVENT_APPLIED_FREQUENCY);
                            if (timeMetricsWriter != null) {
                                timeMetricsWriter.writeMetrics(EVENT, event);
                            }
                        };
                    }
                    return eventApplier;
                }

                @Override
                public EventHandler eventHandler() {
                    final EventHandler eventHandler = singletons.get().eventHandler();
                    if (shouldCapture(EVENT_POLLING_TIME)) {
                        return event -> {
                            captureTime(EVENT_POLLING_TIME);
                            eventHandler.onEvent(event);
                        };
                    }
                    return eventHandler;
                }

                @Override
                public AgentStep eventPollerStep() {
                    return counterStep(EVENT_POLL_FREQUENCY, null, singletons.get().eventPollerStep());
                }
            };
        }
        return null;
    }

    @Override
    public InOutFactory inOutFactory(final Supplier<? extends InOutFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(OUTPUT_PUBLISHED_FREQUENCY) || shouldCaptureAnyOf(OUTPUT)) {
            return new InOutFactory() {
                @Override
                public Input[] inputs() {
                    return singletons.get().inputs();//TODO no interception here?
                }

                @Override
                public Output output() {
                    final Output output = singletons.get().output();
                    if (output == Output.NOOP) {
                        return output;
                    }
                    if (shouldCapture(OUTPUT_PUBLISHED_FREQUENCY) || shouldCaptureAnyOf(OUTPUT)) {
                        return (event, replay, retry) -> {
                            captureTime(OUTPUT_START_TIME);
                            final Ack ack = output.publish(event, replay, retry);
                            if (ack == Ack.IGNORED) {
                                state.clear(OUTPUT_START_TIME);
                            } else {
                                captureTime(OUTPUT_END_TIME);
                                captureCount(OUTPUT_PUBLISHED_FREQUENCY);
                                if (timeMetricsWriter != null) {
                                    timeMetricsWriter.writeMetrics(OUTPUT, event);
                                }
                            }
                            return ack;
                        };
                    }
                    return output;
                }
            };
        }
        return null;
    }

    @Override
    public PublisherFactory publisherFactory(final Supplier<? extends PublisherFactory> singletons) {
        requireNonNull(singletons);
        if (shouldCapture(STEP_ERROR_FREQUENCY) ||
                shouldCapture(OUTPUT_POLLING_TIME) ||
                shouldCapture(OUTPUT_POLL_FREQUENCY)) {
            return new PublisherFactory() {
                @Override
                public OutputHandler outputHandler() {
                    final OutputHandler outputHandler = singletons.get().outputHandler();
                    if (shouldCapture(OUTPUT_POLLING_TIME)) {
                        return (event, replay, retry) -> {
                            captureTime(OUTPUT_POLLING_TIME);
                            return outputHandler.publish(event, replay, retry);
                        };
                    }
                    return outputHandler;
                }

                @Override
                public AgentStep publisherStep() {
                    return counterStep(OUTPUT_POLL_FREQUENCY, null, singletons.get().publisherStep());
                }
            };
        }
        return null;
    }

    private final class TimedEventRouter implements EventRouter.Default {
        final TimedRoutingContext context = new TimedRoutingContext();
        EventRouter router;
        TimedEventRouter init(final EventRouter router) {
            this.router = requireNonNull(router);
            return this;
        }

        @Override
        public RoutingContext routingEvent(final int type) {
            captureTime(ROUTING_START_TIME);
            return context.init(router.routingEvent(type));
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

        @Override
        public Command command() {
            return router.command();
        }
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
            captureTime(ROUTING_END_TIME);
            unclosedContext().route(length);
            context = null;
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

    private SenderSupplier timedSenderSupplier(final SenderSupplier senderSupplier) {
        requireNonNull(senderSupplier);
        return new SenderSupplier() {
            final TimedCommandSender timedCommandSender = new TimedCommandSender();
            @Override
            public CommandSender senderFor(final int source) {
                return timedCommandSender.init(senderSupplier.senderFor(source));
            }

            @Override
            public CommandSender senderFor(final int source, final long sequence) {
                return timedCommandSender.init(senderSupplier.senderFor(source, sequence));
            }
        };
    }

    private final class TimedCommandSender implements CommandSender {
        final DirectBuffer empty = new UnsafeBuffer(0, 0);
        final TimedSendingContext context = shouldCapture(INPUT_SENDING_TIME)
                || shouldCapture(COMMAND_APPENDING_TIME) ? new TimedSendingContext() : null;

        CommandSender sender;

        TimedCommandSender init(final CommandSender sender) {
            this.sender = requireNonNull(sender);
            return this;
        }

        SendingContext sendingContext(final int type, final SendingContext sendingContext) {
            return context != null ? context.init(type, sendingContext) : sendingContext;
        }

        @Override
        public int source() {
            return sender.source();
        }

        @Override
        public long nextCommandSequence() {
            return sender.nextCommandSequence();
        }

        @Override
        public SendingContext sendingCommand() {
            captureTime(INPUT_POLLING_TIME);
            return sendingContext(CommandType.APPLICATION, sender.sendingCommand());
        }

        @Override
        public SendingContext sendingCommand(final int type) {
            captureTime(INPUT_POLLING_TIME);
            return sendingContext(type, sender.sendingCommand(type));
        }

        @Override
        public SendingResult sendCommand(final DirectBuffer buffer, final int offset, final int length) {
            captureTime(INPUT_POLLING_TIME);
            captureInputSendingTime(sender.source(), sender.nextCommandSequence(), CommandType.APPLICATION, buffer, offset, length);
            final SendingResult result = sender.sendCommand(buffer, offset, length);
            captureTime(COMMAND_APPENDING_TIME);
            return result;
        }

        @Override
        public SendingResult sendCommand(final int type, final DirectBuffer buffer, final int offset, final int length) {
            captureTime(INPUT_POLLING_TIME);
            captureInputSendingTime(sender.source(), sender.nextCommandSequence(), type, buffer, offset, length);
            final SendingResult result = sender.sendCommand(type, buffer, offset, length);
            captureTime(COMMAND_APPENDING_TIME);
            return result;
        }

        @Override
        public SendingResult sendCommandWithoutPayload(final int type) {
            captureTime(INPUT_POLLING_TIME);
            captureInputSendingTime(sender.source(), sender.nextCommandSequence(), type, unwrap(empty), 0, 0);
            unwrap(empty);//just in case somebody wraps our buffer
            final SendingResult result = sender.sendCommandWithoutPayload(type);
            captureTime(COMMAND_APPENDING_TIME);
            return result;
        }

        DirectBuffer unwrap(final DirectBuffer buffer) {
            buffer.wrap(0, 0);
            return buffer;
        }
    }

    private final class TimedSendingContext implements SendingContext {
        int type;
        SendingContext context;
        TimedSendingContext init(final int type, final SendingContext context) {
            this.type = type;
            this.context = requireNonNull(context);
            return this;
        }

        SendingContext unclosedContext() {
            if (context != null) {
                return context;
            }
            throw new IllegalStateException("Sending context is closed");
        }

        @Override
        public int source() {
            return unclosedContext().source();
        }

        @Override
        public long sequence() {
            return unclosedContext().sequence();
        }

        @Override
        public MutableDirectBuffer buffer() {
            return unclosedContext().buffer();
        }

        @Override
        public SendingResult send(final int length) {
            final SendingContext sc = captureInputSendingTime(length);
            final SendingResult result = sc.send(length);
            context = null;
            captureTime(COMMAND_APPENDING_TIME);
            return result;
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

        SendingContext captureInputSendingTime(final int length) {
            if (!shouldCapture(INPUT_SENDING_TIME)) {
                return unclosedContext();
            }
            if (length < 0) {
                throw new IllegalArgumentException("Length cannot be negative: " + length);
            }
            final SendingContext sc = unclosedContext();
            MetricsCapturingInterceptor.this.captureInputSendingTime(sc.source(), sc.sequence(), type, sc.buffer(), 0, length);
            return sc;
        }
    }
}
