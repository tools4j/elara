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
package org.tools4j.elara.samples.timer;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2LongCounterMap;
import org.tools4j.elara.application.Application;
import org.tools4j.elara.application.SimpleApplication;
import org.tools4j.elara.chronicle.ChronicleMessageLog;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.EventRouter;
import org.tools4j.elara.flyweight.FlyweightCommand;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.InMemoryLog;
import org.tools4j.elara.plugin.timer.TimerCommands;
import org.tools4j.elara.plugin.timer.TimerEvents;
import org.tools4j.elara.plugin.timer.TimerPlugin;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class TimerApplication {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
    private static int TIMER_TYPE_SINGLE = 1;
    private static int TIMER_TYPE_PERIODIC = 2;
    public static final int MAX_PERIODIC_REPETITIONS = 5;

    private final Application application = new SimpleApplication(
            "timer-app", this::process, this::apply
    );

    private final Long2LongCounterMap periodicState = new Long2LongCounterMap(MAX_PERIODIC_REPETITIONS);

    public ElaraRunner inMemory(final Queue<DirectBuffer> commandQueue) {
        return Elara.launch(Context.create()
                    .input(666, new CommandPoller(commandQueue))
                    .commandLog(new InMemoryLog<>(FlyweightCommand::new))
                    .eventLog(new InMemoryLog<>(FlyweightEvent::new)),
                application,
                new TimerPlugin()
        );
    }

    public ElaraRunner chronicleQueue(final Queue<DirectBuffer> commandQueue,
                                     final String name) {
        final ChronicleQueue cq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/timer/" + name + "-cmd.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        final ChronicleQueue eq = ChronicleQueue.singleBuilder()
                .path("build/chronicle/timer/" + name + "-evt.cq4")
                .wireType(WireType.BINARY_LIGHT)
                .build();
        return Elara.launch(Context.create()
                    .input(666, new CommandPoller(commandQueue))
                    .commandLog(new ChronicleMessageLog<>(cq, FlyweightCommand::new))
                    .eventLog(new ChronicleMessageLog<>(eq, FlyweightEvent::new)),
                application,
                new TimerPlugin()
        );
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
        buffer.putLong(8, timeout);
        return buffer;
    }

    private void process(final Command command, final EventRouter router) {
        System.out.println("-----------------------------------------------------------");
//        System.out.println("processing: " + command + ", payload=" + payloadFor(command.type(), command.payload()));
        if (command.isApplication()) {
            System.out.println("...COMMAND: new timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
            final int timerType = command.payload().getInt(0);
            final long timerId = command.payload().getInt(4);
            final long timeout = command.payload().getLong(8);
            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
            TimerEvents.timerStarted(buffer, 0, timerId, timerType, timeout, router);
        } else if (command.type() == TimerCommands.TRIGGER_TIMER) {
            System.out.println("...COMMAND: trigger timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
        }
    }

    private void apply(final Event event, final CommandLoopback commandLoopback) {
//        System.out.println("applied: " + event + ", payload=" + payloadFor(event.type(), event.payload()));
        if (event.type() == TimerEvents.TIMER_STARTED) {
            final long timerId = TimerEvents.timerId(event);
            final int timerType = TimerEvents.timerType(event);
            final long timeout = TimerEvents.timerTimeout(event);
            System.out.println("...EVENT: timer started: timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
        } else if (event.type() == TimerEvents.TIMER_EXPIRED) {
            final long timerId = TimerEvents.timerId(event);
            final int timerType = TimerEvents.timerType(event);
            final long timeout = TimerEvents.timerTimeout(event);
            if (timerType == TIMER_TYPE_PERIODIC) {
                final long remaining = periodicState.decrementAndGet(timerId);
                if (remaining == 0) {
                    periodicState.remove(timerId);
                    System.out.println("...EVENT: timer expired (periodic end): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
                } else {
                    System.out.println("...EVENT: timer expired (periodic reload, remaining=" + remaining + "): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
                    final DirectBuffer command = startTimerCommand(timerType, timerId, timeout);
                    commandLoopback.enqueueCommand(command, 0, command.capacity());
                }
            } else {
                System.out.println("...EVENT: timer expired (single): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
            }
        }
    }

    private static String formatTime(final long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER);
    }

    private String payloadFor(final int type, final DirectBuffer payload) {
        if (type == 0) {
            final int timerType = payload.getInt(0);
            final long timerId = payload.getLong(4);
            final long timeout = payload.getLong(8);
            return "timer-type=" + timerType + ", timerId=" + timerId + ", timeout=" + timeout;
        }
        return "(unknown)";
    }

    private static class CommandPoller implements Input.Poller {
        final Queue<DirectBuffer> commands;
        long seq = 0;

        CommandPoller(final Queue<DirectBuffer> commands) {
            this.commands = requireNonNull(commands);
        }

        @Override
        public int poll(final Input.Handler handler) {
            final DirectBuffer command = commands.poll();
            if (command != null) {
                handler.onMessage(++seq, command, 0, command.capacity());
                return 1;
            }
            return 0;
        }
    }
}