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
///**
// * The MIT License (MIT)
// *
// * Copyright (c) 2020 tools4j.org (Marco Terzer, Anton Anufriev)
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all
// * copies or substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// * SOFTWARE.
// */
//package org.tools4j.elara.samples.timer;
//
//import net.openhft.chronicle.queue.ChronicleQueue;
//import net.openhft.chronicle.wire.WireType;
//import org.agrona.DirectBuffer;
//import org.agrona.ExpandableArrayBuffer;
//import org.agrona.MutableDirectBuffer;
//import org.agrona.collections.Long2LongCounterMap;
//import org.tools4j.elara.application.Application;
//import org.tools4j.elara.application.SimpleApplication;
//import org.tools4j.elara.chronicle.ChronicleMessageLog;
//import org.tools4j.elara.command.Command;
//import org.tools4j.elara.event.Event;
//import org.tools4j.elara.flyweight.FlyweightEvent;
//import org.tools4j.elara.init.Context;
//import org.tools4j.elara.input.Input;
//import org.tools4j.elara.input.Receiver;
//import org.tools4j.elara.log.InMemoryLog;
//import org.tools4j.elara.output.CommandLoopback;
//import org.tools4j.elara.plugin.timer.TimerCommands;
//import org.tools4j.elara.plugin.timer.TimerEvents;
//import org.tools4j.elara.plugin.timer.TimerPlugin;
//import org.tools4j.elara.route.EventRouter;
//import org.tools4j.elara.route.EventRouter.RoutingContext;
//import org.tools4j.elara.run.Elara;
//import org.tools4j.elara.run.ElaraRunner;
//
//import java.time.Instant;
//import java.time.ZoneId;
//import java.time.format.DateTimeFormatter;
//import java.util.Queue;
//import java.util.function.Consumer;
//
//import static java.util.Objects.requireNonNull;
//import static org.tools4j.elara.plugin.timer.PeriodicTimers.PERIODIC_REPETITION;
//import static org.tools4j.elara.plugin.timer.PeriodicTimers.SINGLE_REPETITION;
//
//public class TimerApplication {
//
//    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
//    public static int TIMER_TYPE_SINGLE = 1;
//    public static int TIMER_TYPE_PERIODIC = 2;
//    public static final int PERIODIC_REPETITIONS = 5;
//
//    private final Long2LongCounterMap periodicCounters = new Long2LongCounterMap(0);
//
//    public ElaraRunner inMemory(final Queue<DirectBuffer> commandQueue,
//                                final Consumer<? super Event> eventConsumer) {
//        return Elara.launch(Context.create()
//                    .input(666, new CommandPoller(commandQueue))
//                    .output(this::publish)
//                    .commandLog(new InMemoryLog())
//                    .eventLog(new InMemoryLog()),
//                application(eventConsumer),
//                new TimerPlugin()
//        );
//    }
//
//    public ElaraRunner chronicleQueue(final Queue<DirectBuffer> commandQueue,
//                                      final String queueName,
//                                      final Consumer<? super Event> eventConsumer) {
//        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
//                .path("build/chronicle/timer/" + queueName + "-cmd.cq4")
//                .wireType(WireType.BINARY_LIGHT)
//                .build();
//        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
//                .path("build/chronicle/timer/" + queueName + "-evt.cq4")
//                .wireType(WireType.BINARY_LIGHT)
//                .build();
//        return Elara.launch(Context.create()
//                    .input(666, new CommandPoller(commandQueue))
//                    .output(this::publish)
//                    .commandLog(new ChronicleMessageLog(cq))
//                    .eventLog(new ChronicleMessageLog(eq)),
//                application(eventConsumer),
//                new TimerPlugin()
//        );
//    }
//
//    private Application application(final Consumer<? super Event> eventConsumer) {
//        requireNonNull(eventConsumer);
//        return new SimpleApplication("timer-app", this::process, event -> {
//            this.apply(event);
//            eventConsumer.accept(cloneEvent(event));
//        });
//    }
//
//    public static DirectBuffer startTimer(final long timerId, final long timeoutMillis) {
//        return startTimerCommand(TIMER_TYPE_SINGLE, timerId, timeoutMillis);
//    }
//
//    public static DirectBuffer startPeriodic(final long timerId, final long periodMillis) {
//        return startTimerCommand(TIMER_TYPE_PERIODIC, timerId, periodMillis);
//    }
//
//    private static DirectBuffer startTimerCommand(final int timerType,
//                                                  final long timerId,
//                                                  final long timeout) {
//        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(Integer.BYTES + Long.BYTES + Long.BYTES);
//        buffer.putInt(0, timerType);
//        buffer.putLong(4, timerId);
//        buffer.putLong(12, timeout);
//        return buffer;
//    }
//
//    private void process(final Command command, final EventRouter router) {
//        System.out.println("-----------------------------------------------------------");
////        System.out.println("processing: " + command + ", payload=" + payloadFor(command.type(), command.payload()));
//        if (command.isApplication()) {
//            System.out.println("...COMMAND: new timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
//            final int timerType = command.payload().getInt(0);
//            final long timerId = command.payload().getInt(4);
//            final long timeout = command.payload().getLong(12);
//            final int repetition = timerType == TIMER_TYPE_PERIODIC ? PERIODIC_REPETITION : SINGLE_REPETITION;
//            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
//            try (final RoutingContext context = router.routingEvent(TimerEvents.TIMER_STARTED)) {
//                final int length = TimerEvents.timerStarted(buffer, 0, timerId, timerType, repetition, timeout);
//                context.route(length);
//            }
//        } else if (command.type() == TimerCommands.TRIGGER_TIMER) {
//            System.out.println("...COMMAND: trigger timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
//        }
//    }
//
//    private void apply(final Event event) {
////        System.out.println("applied: " + event + ", payload=" + payloadFor(event.type(), event.payload()));
//        if (event.type() == TimerEvents.TIMER_STARTED) {
//            final long timerId = TimerEvents.timerId(event);
//            final int timerType = TimerEvents.timerType(event);
//            final long timeout = TimerEvents.timerTimeout(event);
//            if (timerType == TIMER_TYPE_PERIODIC) {
//                final long iteration = periodicState.get(timerId);
//                if (iteration == PERIODIC_REPETITIONS) {
//                    periodicState.remove(timerId);
//                } else {
//                    periodicState.put(timerId, iteration + 1);
//                }
//            }
//            System.out.println("...EVENT: timer started: timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
//        } else if (event.type() == TimerEvents.TIMER_EXPIRED) {
//            final long timerId = TimerEvents.timerId(event);
//            final int timerType = TimerEvents.timerType(event);
//            final long timeout = TimerEvents.timerTimeout(event);
//            if (timerType == TIMER_TYPE_PERIODIC) {
//                final long iteration = periodicState.get(timerId);
//                System.out.println("...EVENT: timer expired (periodic, iteration=" + iteration + "): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
//            } else {
//                System.out.println("...EVENT: timer expired (single): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
//            }
//        }
//    }
//
//    private void publish(final Event event, final boolean replay, final CommandLoopback loopback) {
//        if (replay) {
//            return;
//        }
//        if (event.type() == TimerEvents.TIMER_EXPIRED) {
//            final long timerId = TimerEvents.timerId(event);
//            final int timerType = TimerEvents.timerType(event);
//            final long timeout = TimerEvents.timerTimeout(event);
//            if (timerType == TIMER_TYPE_PERIODIC) {
//                final long iteration = periodicState.get(timerId);
//                if (iteration < PERIODIC_REPETITIONS) {
//                    final DirectBuffer command = startTimerCommand(timerType, timerId, timeout);
//                    loopback.enqueueCommand(command, 0, command.capacity());
//                    System.out.println("...PUBLISH: timer reload command enqueued (periodic, iteration=" + iteration + "): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
//                }
//            }
//        }
//    }
//
//    private static String formatTime(final long time) {
//        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
//    }
//
//    private String payloadFor(final int type, final DirectBuffer payload) {
//        if (type == 0) {
//            final int timerType = payload.getInt(0);
//            final long timerId = payload.getLong(4);
//            final long timeout = payload.getLong(12);
//            return "timer-type=" + timerType + ", timerId=" + timerId + ", timeout=" + timeout;
//        }
//        return "(unknown)";
//    }
//
//    private static Event cloneEvent(final Event event) {
//        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
//        event.writeTo(buffer, 0);
//        return new FlyweightEvent().init(buffer, 0);
//    }
//
//    private static class CommandPoller implements Input.Poller {
//        final Queue<DirectBuffer> commands;
//        long seq = 0;
//
//        CommandPoller(final Queue<DirectBuffer> commands) {
//            this.commands = requireNonNull(commands);
//        }
//
//        @Override
//        public int poll(final Receiver receiver) {
//            final DirectBuffer command = commands.poll();
//            if (command != null) {
//                receiver.receiveMessage(++seq, command, 0, command.capacity());
//                return 1;
//            }
//            return 0;
//        }
//    }
//}