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
package org.tools4j.elara.samples.timer;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.config.Context;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.timer.SimpleTimerState;
import org.tools4j.elara.plugin.timer.TimerCommands;
import org.tools4j.elara.plugin.timer.TimerEvents;
import org.tools4j.elara.plugin.timer.TimerState;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.store.InMemoryStore;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class TimerApplication {

    private static final int SOURCE = 777;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
    public static final int TIMER_TYPE_SINGLE = 1;
    public static final int TIMER_TYPE_PERIODIC = 2;
    public static final int PERIODIC_REPETITIONS = 5;

    public ElaraRunner inMemory(final Queue<DirectBuffer> commandQueue,
                                final Consumer<? super Event> eventConsumer) {
        return inMemory(commandQueue, eventConsumer, () -> new SimpleTimerState());
    }

    public ElaraRunner inMemory(final Queue<DirectBuffer> commandQueue,
                                final Consumer<? super Event> eventConsumer,
                                final Supplier<? extends TimerState.Mutable> timerStateSupplier) {
        return Elara.launch(Context.create()
                .commandProcessor(this::process)
                .eventApplier(eventApplier(eventConsumer))
                .input(new CommandInput(commandQueue))
                .commandStore(new InMemoryStore())
                .eventStore(new InMemoryStore())
                .plugin(Plugins.timerPlugin(), timerStateSupplier)
        );
    }

    public ElaraRunner chronicleQueue(final Queue<DirectBuffer> commandQueue,
                                      final String queueName,
                                      final Consumer<? super Event> eventConsumer) {
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/timer/" + queueName + "-cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/timer/" + queueName + "-evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                .commandProcessor(this::process)
                .eventApplier(eventApplier(eventConsumer))
                .input(new CommandInput(commandQueue))
                .commandStore(new ChronicleMessageStore(cq))
                .eventStore(new ChronicleMessageStore(eq))
                .plugin(Plugins.timerPlugin())
        );
    }

    private EventApplier eventApplier(final Consumer<? super Event> eventConsumer) {
        requireNonNull(eventConsumer);
        return event -> {
            apply(event);
            eventConsumer.accept(cloneEvent(event));
        };
    }

    public static DirectBuffer startTimer(final long timerId, final long timeoutMillis) {
        return startTimerCommand(TIMER_TYPE_SINGLE, timerId, timeoutMillis);
    }

    public static DirectBuffer startPeriodic(final long timerId, final long periodMillis) {
        return startTimerCommand(TIMER_TYPE_PERIODIC, timerId, periodMillis);
    }

    private static DirectBuffer startTimerCommand(final int timerType,
                                                  final long timerId,
                                                  final long timeout) {
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(Integer.BYTES + Long.BYTES + Long.BYTES);
        buffer.putInt(0, timerType);
        buffer.putLong(4, timerId);
        buffer.putLong(12, timeout);
        return buffer;
    }

    private void process(final Command command, final EventRouter router) {
        System.out.println("-----------------------------------------------------------");
//        System.out.println("processing: " + command + ", payload=" + payloadFor(command.type(), command.payload()));
        if (command.isApplication()) {
            System.out.println("...COMMAND: new timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
            final int timerType = command.payload().getInt(0);
            final long timerId = command.payload().getInt(4);
            final long timeout = command.payload().getLong(12);
            try (final RoutingContext context = router.routingEvent(TimerEvents.TIMER_STARTED)) {
                final int length;
                if (timerType == TIMER_TYPE_PERIODIC) {
                    length = TimerEvents.periodicStarted(context.buffer(), 0, timerId, timerType, timeout);
                } else {
                    length = TimerEvents.timerStarted(context.buffer(), 0, timerId, timerType, timeout);
                }
                context.route(length);
            }
        } else if (command.type() == TimerCommands.TRIGGER_TIMER) {
            final long timerId = TimerCommands.timerId(command);
            final int timerType = TimerCommands.timerType(command);
            final int repetition = TimerCommands.timerRepetition(command);
            final long timeout = TimerCommands.timerTimeout(command);
            System.out.println("...COMMAND: trigger timer: timerId=" + timerId + ", timerType=" + timerType + ", repetition=" + repetition + ", timeout=" + timeout + ", time=" + formatTime(command.time()));
            if (TimerCommands.timerRepetition(command) >= PERIODIC_REPETITIONS) {
                try (final RoutingContext context = router.routingEvent(TimerEvents.TIMER_STOPPED)) {
                    final int length = TimerEvents.timerStopped(context.buffer(), 0, timerId, timerType, repetition, timeout);
                    context.route(length);
                }
            }
        }
    }

    private void apply(final Event event) {
//        System.out.println("applied: " + event + ", payload=" + payloadFor(event.type(), event.payload()));
        if (TimerEvents.isTimerEvent(event)) {
            final String name = TimerEvents.timerEventName(event);
            final long timerId = TimerEvents.timerId(event);
            final int timerType = TimerEvents.timerType(event);
            final int repetition = TimerEvents.timerRepetition(event);
            final long timeout = TimerEvents.timerTimeout(event);
            System.out.println("...EVENT: " + name + ": timerId=" + timerId + ", timerType=" + timerType + ", repetition=" + repetition + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
        }
    }

    private static String formatTime(final long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    private String payloadFor(final int type, final DirectBuffer payload) {
        if (type == 0) {
            final int timerType = payload.getInt(0);
            final long timerId = payload.getLong(4);
            final long timeout = payload.getLong(12);
            return "timer-type=" + timerType + ", timerId=" + timerId + ", timeout=" + timeout;
        }
        return "(unknown)";
    }

    private static Event cloneEvent(final Event event) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        event.writeTo(buffer, 0);
        return new FlyweightEvent().init(buffer, 0);
    }

    private static class CommandInput implements Input {
        final Queue<DirectBuffer> commands;

        CommandInput(final Queue<DirectBuffer> commands) {
            this.commands = requireNonNull(commands);
        }

        @Override
        public int poll(final SenderSupplier senderSupplier) {
            final DirectBuffer command = commands.poll();
            if (command != null) {
                senderSupplier.senderFor(SOURCE).sendCommand(command, 0, command.capacity());
                return 1;
            }
            return 0;
        }
    }
}