/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.plugin.timer;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.timer.Timer.Style;
import org.tools4j.elara.plugin.timer.TimerController.ControlContext;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.route.EventRouter.RoutingContext;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.CommandSender.SendingContext;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.FlyweightTimerPayload.writeAlarm;
import static org.tools4j.elara.plugin.timer.FlyweightTimerPayload.writePeriodic;
import static org.tools4j.elara.plugin.timer.FlyweightTimerPayload.writeTimer;
import static org.tools4j.elara.plugin.timer.TimerCommands.CANCEL_TIMER;
import static org.tools4j.elara.plugin.timer.TimerCommands.START_TIMER;
import static org.tools4j.elara.plugin.timer.TimerEvents.TIMER_CANCELLED;
import static org.tools4j.elara.plugin.timer.TimerEvents.TIMER_STARTED;

final class FlyweightTimerController implements ControlContext {

    private final CommandTimerController commandController = new CommandTimerController();
    private final EventTimerController eventController = new EventTimerController();

    private TimeSource timeSource;
    private TimerIdGenerator timerIdGenerator;
    private TimerState timerState;

    void init(final TimeSource timeSource, final TimerIdGenerator timerIdGenerator, final TimerState timerState) {
        this.timeSource = requireNonNull(timeSource);
        this.timerIdGenerator = requireNonNull(timerIdGenerator);
        this.timerState = requireNonNull(timerState);
    }

    TimerState timerState() {
        return timerState;
    }

    private void checkInitialized() {
        if (timeSource == null | timerIdGenerator == null | timerState == null) {
            throw new IllegalStateException("TimerController is not yet initialized");
        }
    }

    ControlContext init(final Event event, final CommandSender commandSender) {
        eventController.close();
        commandController.event = event;
        commandController.commandSender = commandSender;
        return commandController;
    }

    ControlContext init(final EventRouter eventRouter) {
        commandController.close();
        eventController.eventRouter = eventRouter;
        return eventController;
    }

    @Override
    public long currentTime() {
        checkInitialized();
        if (eventController.ready()) {
            return eventController.currentTime();
        }
        if (commandController.ready()) {
            return commandController.currentTime();
        }
        throw illegalStateException();
    }

    @Override
    public long startAlarm(final long time, final int type, final long contextId) {
        checkInitialized();
        if (eventController.ready()) {
            return eventController.startAlarm(time, type, contextId);
        }
        if (commandController.ready()) {
            return commandController.startAlarm(time, type, contextId);
        }
        throw illegalStateException();
    }

    @Override
    public long startTimer(final long timeout, final int type, final long contextId) {
        checkInitialized();
        if (eventController.ready()) {
            return eventController.startTimer(timeout, type, contextId);
        }
        if (commandController.ready()) {
            return commandController.startTimer(timeout, type, contextId);
        }
        throw illegalStateException();
    }

    @Override
    public long startPeriodic(final long timeout, final int type, final long contextId) {
        checkInitialized();
        if (eventController.ready()) {
            return eventController.startPeriodic(timeout, type, contextId);
        }
        if (commandController.ready()) {
            return commandController.startPeriodic(timeout, type, contextId);
        }
        throw illegalStateException();
    }

    @Override
    public boolean cancelTimer(final long id) {
        checkInitialized();
        if (eventController.ready()) {
            return eventController.cancelTimer(id);
        }
        if (commandController.ready()) {
            return eventController.cancelTimer(id);
        }
        throw illegalStateException();
    }

    @Override
    public void close() {
        commandController.close();
        eventController.close();
    }

    private int writeCancelPayload(final long timerId, final int timerIndex, final MutableDirectBuffer dst, final int dstOffset) {
        assert timerId == timerState.timerId(timerIndex);
        final Style style = timerState.style(timerIndex);
        switch (style) {
            case ALARM:
                return writeAlarm(timerId, timerState.timeout(timerIndex), timerState.timerType(timerIndex),
                        timerState.contextId(timerIndex), dst, dstOffset);
            case TIMER:
                return writeTimer(timerId, timerState.timeout(timerIndex), timerState.timerType(timerIndex),
                        timerState.contextId(timerIndex), dst, dstOffset);
            case PERIODIC:
                return writePeriodic(timerId, timerState.timeout(timerIndex), timerState.repetition(timerIndex),
                        timerState.timerType(timerIndex), timerState.contextId(timerIndex), dst, dstOffset);
            default:
                throw new IllegalStateException("Timer " + timerId + " has invalid style: " + style);
        }
    }

    private static IllegalStateException illegalStateException() {
        throw new IllegalStateException("Timer controller can only be used during the processing of a command, and event or with a valid source context");
    }

    private final class EventTimerController implements ControlContext {
        EventRouter eventRouter;

        private EventRouter eventRouter() {
            checkInitialized();
            if (eventRouter != null) {
                return eventRouter;
            }
            throw new IllegalStateException("Timer controller can only be used during the processing of a command");
        }

        boolean ready() {
            return eventRouter != null;
        }

        @Override
        public long currentTime() {
            checkInitialized();
            return timeSource.currentTime();
        }

        @Override
        public long startAlarm(final long time, final int type, final long contextId) {
            final EventRouter router = eventRouter();
            final int sourceId = router.command().sourceId();
            final long timerId = timerIdGenerator.current(sourceId);
            try (final RoutingContext context = eventRouter.routingEvent(TIMER_STARTED)) {
                final int length = writeAlarm(timerId, time, type, contextId, context.buffer(), 0);
                context.route(length);
            }
            timerIdGenerator.next(timerId + 1);
            return timerId;
        }

        @Override
        public long startTimer(final long timeout, final int type, final long contextId) {
            final EventRouter router = eventRouter();
            final int sourceId = router.command().sourceId();
            final long timerId = timerIdGenerator.current(sourceId);
            try (final RoutingContext context = eventRouter.routingEvent(TIMER_STARTED)) {
                final int length = writeTimer(timerId, timeout, type, contextId, context.buffer(), 0);
                context.route(length);
            }
            timerIdGenerator.next(timerId + 1);
            return timerId;
        }

        @Override
        public long startPeriodic(final long timeout, final int type, final long contextId) {
            final EventRouter router = eventRouter();
            final int sourceId = router.command().sourceId();
            final long timerId = timerIdGenerator.current(sourceId);
            try (final RoutingContext context = eventRouter.routingEvent(TIMER_STARTED)) {
                final int length = writePeriodic(timerId, timeout, 0, type, contextId, context.buffer(), 0);
                context.route(length);
            }
            timerIdGenerator.next(timerId + 1);
            return timerId;
        }

        @Override
        public boolean cancelTimer(final long id) {
            final int timerIndex = timerState.index(id);
            if (timerIndex >= 0) {
                try (final RoutingContext context = eventRouter().routingEvent(TIMER_CANCELLED)) {
                    final int length = writeCancelPayload(id, timerIndex, context.buffer(), 0);
                    context.route(length);
                }
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            eventRouter = null;
        }
    }

    private final class CommandTimerController implements ControlContext {
        Event event;//can be null even if the controller is initialized
        CommandSender commandSender;

        private CommandSender commandSender() {
            checkInitialized();
            if (commandSender != null) {
                return commandSender;
            }
            throw new IllegalStateException("Timer controller can only be used during the processing of an event or when a command sender is available");
        }

        boolean ready() {
            return commandSender != null;
        }

        @Override
        public long currentTime() {
            checkInitialized();
            return event != null ? event.eventTime() : timeSource.currentTime();
        }

        @Override
        public long startAlarm(final long time, final int type, final long contextId) {
            final CommandSender commandSender = commandSender();
            final long timerId = timerIdGenerator.current(commandSender.sourceId());
            try (final SendingContext context = commandSender.sendingCommand(START_TIMER)) {
                final int length = writeAlarm(timerId, time, type, contextId, context.buffer(), 0);
                context.send(length);
            }
            timerIdGenerator.next(timerId + 1);
            return timerId;
        }

        @Override
        public long startTimer(final long timeout, final int type, final long contextId) {
            final CommandSender commandSender = commandSender();
            final long timerId = timerIdGenerator.current(commandSender.sourceId());
            try (final SendingContext context = commandSender.sendingCommand(START_TIMER)) {
                final int length = writeTimer(timerId, timeout, type, contextId, context.buffer(), 0);
                context.send(length);
            }
            timerIdGenerator.next(timerId + 1);
            return timerId;
        }

        @Override
        public long startPeriodic(final long timeout, final int type, final long contextId) {
            final CommandSender commandSender = commandSender();
            final long timerId = timerIdGenerator.current(commandSender.sourceId());
            try (final SendingContext context = commandSender.sendingCommand(START_TIMER)) {
                final int length = writePeriodic(timerId, timeout, 0, type, contextId, context.buffer(), 0);
                context.send(length);
            }
            timerIdGenerator.next(timerId + 1);
            return timerId;
        }

        @Override
        public boolean cancelTimer(final long id) {
            final int timerIndex = timerState.index(id);
            if (timerIndex >= 0) {
                try (final SendingContext context = commandSender().sendingCommand(CANCEL_TIMER)) {
                    final int length = writeCancelPayload(id, timerIndex, context.buffer(), 0);
                    context.send(length);
                }
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            event = null;
            commandSender = null;
        }
    }
}