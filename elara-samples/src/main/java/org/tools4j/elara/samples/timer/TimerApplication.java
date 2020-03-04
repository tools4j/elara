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

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2LongCounterMap;
import org.tools4j.elara.application.Application;
import org.tools4j.elara.application.SimpleApplication;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.command.CommandType;
import org.tools4j.elara.command.FlyweightCommand;
import org.tools4j.elara.event.*;
import org.tools4j.elara.init.Context;
import org.tools4j.elara.init.Launcher;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.InMemoryLog;
import org.tools4j.elara.plugin.TimerPlugin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Queue;

import static java.util.Objects.requireNonNull;

public class TimerApplication {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
    private static long TIMER_ID_OFFSET = 1000000000;
    private static int TIMER_TYPE_SINGLE = 1;
    private static int TIMER_TYPE_PERIODIC = 2;
    private static final int MAX_PERIODIC_REPETITIONS = 10;

    private final Application application = new SimpleApplication(
            "timer-app", this::process, this::apply
    );

    private final Long2LongCounterMap periodicState = new Long2LongCounterMap(MAX_PERIODIC_REPETITIONS);

    public Launcher launch(final Queue<DirectBuffer> commandQueue) {
        return Launcher.launch(Context.create(application)
                .plugin(new TimerPlugin())
                .input(666, new CommandPoller(commandQueue))
                .commandLog(new InMemoryLog<>(new FlyweightCommand()))
                .eventLog(new InMemoryLog<>(new FlyweightEvent()))
        );
    }

    public static DirectBuffer startTimer(final long timeoutMillis) {
        return startTimerCommand(TIMER_TYPE_SINGLE, timeoutMillis);
    }

    public static DirectBuffer startPeriodic(final long periodMillis) {
        return startTimerCommand(TIMER_TYPE_PERIODIC, periodMillis);
    }

    private static DirectBuffer startTimerCommand(final int timerType,
                                                  final long timeout) {
        final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(Integer.BYTES + Long.BYTES);
        buffer.putInt(0, timerType);
        buffer.putLong(4, timeout);
        return buffer;
    }

    private void process(final Command command, final EventRouter router) {
        System.out.println("-----------------------------------------------------------");
//        System.out.println("processing: " + command + ", payload=" + payloadFor(command.type(), command.payload()));
        if (command.isApplication()) {
            System.out.println("...COMMAND: new timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
            final int timerType = command.payload().getInt(0);
            final long timerId = TIMER_ID_OFFSET + command.id().sequence();
            final long timeout = command.payload().getLong(4);
            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
            AdminEvents.timerStarted(buffer, 0, timerType, timerId, timeout, router);
        } else if (command.type() == CommandType.TRIGGER_TIMER.value()) {
            System.out.println("...COMMAND: trigger timer: " + command + ", payload=" + payloadFor(command.type(), command.payload()) + ", time=" + formatTime(command.time()));
        }
    }

    private void apply(final Event event, final CommandLoopback commandLoopback) {
//        System.out.println("applied: " + event + ", payload=" + payloadFor(event.type(), event.payload()));
        if (event.type() == EventType.TIMER_STARTED.value()) {
            final long timerId = AdminEvents.timerId(event);
            final int timerType = AdminEvents.timerType(event);
            final long timeout = AdminEvents.timerTimeout(event);
            System.out.println("...EVENT: timer started: timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
        } else if (event.type() == EventType.TIMER_EXPIRED.value()) {
            final long timerId = AdminEvents.timerId(event);
            final int timerType = AdminEvents.timerType(event);
            final long timeout = AdminEvents.timerTimeout(event);
            if (timerType == TIMER_TYPE_PERIODIC) {
                if (periodicState.decrementAndGet(timerId) == 0) {
                    System.out.println("...EVENT: timer expired (periodic end): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
                } else {
                    System.out.println("...EVENT: timer expired (periodic reload): timerId=" + timerId + ", timerType=" + timerType + ", timeout=" + timeout + ", time=" + formatTime(event.time()));
                    final DirectBuffer command = startTimerCommand(timerType, timeout);
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
            final long timeout = payload.getLong(4);
            return "timer-type=" + timerType + ", timeout=" + timeout;
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