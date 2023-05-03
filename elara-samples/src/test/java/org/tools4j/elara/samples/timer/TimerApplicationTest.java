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
package org.tools4j.elara.samples.timer;

import org.junit.jupiter.api.Test;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.plugin.timer.DeadlineHeapTimerState;
import org.tools4j.elara.plugin.timer.FlyweightTimerPayload;
import org.tools4j.elara.plugin.timer.MutableTimerState;
import org.tools4j.elara.plugin.timer.SimpleTimerState;
import org.tools4j.elara.plugin.timer.Timer.Style;
import org.tools4j.elara.plugin.timer.TimerController.ControlContext;
import org.tools4j.elara.plugin.timer.TimerEvents;
import org.tools4j.elara.plugin.timer.TimerIdGenerator;
import org.tools4j.elara.run.ElaraRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tools4j.elara.samples.timer.TimerApplication.PERIODIC_REPETITIONS;
import static org.tools4j.elara.samples.timer.TimerApplication.oneTimeInput;

/**
 * Unit test for {@link TimerApplication}.
 */
class TimerApplicationTest {

    @Test
    public void singleTimers() {
        singleTimers(false, true);
    }

    @Test
    public void singleTimersWithDeadlineHeap() {
        singleTimers(false, false);
    }

    @Test
    public void singleTimersPersisted() {
        singleTimers(true, true);
    }

    private static long timerId(final int index) {
        return TimerIdGenerator.timerId(TimerApplication.SOURCE_ID, index);
    }

    private void singleTimers(final boolean persisted, final boolean simpleState) {
        //given
        final int[] microTimeouts = {200_000, 500_000, 800_000, 1000_000};
        final int[] types = {101, 102, 103, 104};
        final long[] contextIds = {100000000001L, 100000000002L, 100000000003L, 100000000004L};
        final TimerApplication app = new TimerApplication();
        final Supplier<? extends MutableTimerState> timerStateSupplier = simpleState ?
                SimpleTimerState::new : DeadlineHeapTimerState::new;
        final long[] startTimePtr = {0};//NOTE: if we initialize this here, ALARM timers may go off out of sequence
        final SingleSourceInput input = (sender, commandTracker) -> {
            try (final ControlContext timerControl = app.timerPlugin.controller(sender)) {
                if (startTimePtr[0] == 0) {
                    startTimePtr[0] = timerControl.currentTime();
                }
                timerControl.startTimer(microTimeouts[0], types[0], contextIds[0]);
                timerControl.startTimer(microTimeouts[1], types[1], contextIds[1]);
                timerControl.startAlarm(startTimePtr[0] + microTimeouts[2], types[2], contextIds[2]);
                timerControl.startAlarm(startTimePtr[0] + microTimeouts[3], types[3], contextIds[3]);
            }
            return 4;
        };
        final List<Event> events = new ArrayList<>();

        //when
        try (final ElaraRunner runner = persisted ?
                app.chronicleQueue(oneTimeInput(input), "single", events::add) :
                app.inMemory(oneTimeInput(input), events::add, timerStateSupplier)) {
            runner.join(3000);
        }

        //then
        final FlyweightTimerPayload timer = new FlyweightTimerPayload();
        final BiConsumer<String, List<Event>> asserter = (name, evts) -> {
            System.out.println("===========================================================");
            System.out.println("asserting " + name + " events at " + Instant.now());
            assertEquals(8, evts.size(), "events.size");
            for (int i = 0; i < microTimeouts.length; i++) {
                final int iStarted = i;
                final int iSignalled = i + 4;
                final Event started = evts.get(iStarted);
                timer.wrap(started.payload(), 0);
                assertEquals(TimerEvents.TIMER_STARTED, started.payloadType(), "events[" + iStarted + "].payloadType");
                assertEquals(timerId(i + 1), timer.timerId(), "events[" + iStarted + "].timerId");
                assertEquals(types[i], timer.timerType(), "events[" + iStarted + "].timerType");
                assertEquals(contextIds[i], timer.contextId(), "events[" + iStarted + "].contextId");
                if (i < 2) {
                    assertEquals(Style.TIMER, timer.style(), "events[" + iStarted + "].style");
                    assertEquals(microTimeouts[i], timer.timeout(), "events[" + iStarted + "].timeout");
                } else {
                    assertEquals(Style.ALARM, timer.style(), "events[" + iStarted + "].style");
                    assertEquals(startTimePtr[0] + microTimeouts[i], timer.timeout(), "events[" + iStarted + "].timeout");
                }

                final Event signalled = evts.get(iSignalled);
                timer.wrap(signalled.payload(), 0);
                assertEquals(TimerEvents.TIMER_SIGNALLED, signalled.payloadType(), "events[" + iSignalled + "].payloadType");
                assertEquals(timerId(i + 1), timer.timerId(), "events[" + iSignalled + "].timerId");
                assertEquals(types[i], timer.timerType(), "events[" + iSignalled + "].timerType");
                assertEquals(contextIds[i], timer.contextId(), "events[" + iSignalled + "].contextId");
                if (i < 2) {
                    assertEquals(Style.TIMER, timer.style(), "events[" + iSignalled + "].style");
                    assertEquals(microTimeouts[i], timer.timeout(), "events[" + iSignalled + "].timeout");
                } else {
                    assertEquals(Style.ALARM, timer.style(), "events[" + iSignalled + "].style");
                    assertEquals(startTimePtr[0] + microTimeouts[i], timer.timeout(), "events[" + iSignalled + "].timeout");
                }
            }
        };
        asserter.accept("original", events);
        System.out.println(events.size() + " events asserted");

        if (persisted) {
            System.out.println("-----------------------------------------------------------");
            System.out.println("starting replay");

            //when
            final List<Event> replay = new ArrayList<>();
            try (final ElaraRunner runner = app.chronicleQueue(oneTimeInput(input), "single", replay::add)) {
                runner.join(1000);
            }

            //then
            asserter.accept("replay", replay);
            for (int i = 0; i < events.size(); i++) {
                assertEquals(events.get(i).eventTime(), replay.get(i).eventTime(), "events[" + i + "].time == replay[" + i + "].time");
            }
            System.out.println(replay.size() + " replay events asserted");
        }
    }

    @Test
    public void periodicTimer() {
        periodicTimer(false, true);
    }

    @Test
    public void periodicTimerWithDeadlineHeap() {
        periodicTimer(false, false);
    }

    @Test
    public void periodicTimerPersisted() {
        periodicTimer(true, true);
    }

    private void periodicTimer(final boolean persisted, final boolean simpleState) {
        //given
        final long timerId = timerId(1);
        final int timerType = 12345;
        final long contextId = 6666666666666L;
        final long periodMillis = 500;
        final long periodMicros = periodMillis * 1000;
        final TimerApplication app = new TimerApplication();
        final Supplier<? extends MutableTimerState> timerStateSupplier = simpleState ?
                SimpleTimerState::new : DeadlineHeapTimerState::new;
        final List<Event> events = new ArrayList<>();

        //when
        final SingleSourceInput input = (sender, commandTracker) -> {
            try (final ControlContext timerControl = app.timerPlugin.controller(sender)) {
                timerControl.startPeriodic(periodMicros, timerType, contextId);
            }
            return 1;
        };

        try (final ElaraRunner runner = persisted ?
                app.chronicleQueue(oneTimeInput(input), "periodic", events::add) :
                app.inMemory(oneTimeInput(input), events::add, timerStateSupplier)) {

            //then
            runner.join(2000 + periodMillis * (PERIODIC_REPETITIONS + 1));
        }

        //then
        final FlyweightTimerPayload timer = new FlyweightTimerPayload();
        final BiConsumer<String, List<Event>> asserter = (name, evts) -> {
            System.out.println("===========================================================");
            System.out.println("asserting " + name + " events at " + Instant.now());
            assertEquals(PERIODIC_REPETITIONS + 2, evts.size(), "events.size");

            final int iStarted = 0;
            final Event started = evts.get(iStarted);
            timer.wrap(started.payload(), 0);
            assertEquals(TimerEvents.TIMER_STARTED, started.payloadType(), "events[" + iStarted + "].payloadType");
            assertEquals(timerId, timer.timerId(), "events[" + iStarted + "].timerId");
            assertEquals(0, timer.repetition(), "events[" + iStarted + "].repetition");
            assertEquals(periodMicros, timer.timeout(), "events[" + iStarted + "].timeout");
            assertEquals(timerType, timer.timerType(), "events[" + iStarted + "].timerType");
            assertEquals(contextId, timer.contextId(), "events[" + iStarted + "].contextId");
            for (int repetition = 1; repetition <= PERIODIC_REPETITIONS; repetition++) {
                final int iSignalled = repetition;
                final Event signalled = evts.get(iSignalled);
                timer.wrap(signalled.payload(), 0);
                assertEquals(TimerEvents.TIMER_SIGNALLED, signalled.payloadType(), "events[" + iSignalled + "].payloadType");
                assertEquals(timerId, timer.timerId(), "events[" + iSignalled + "].timerId");
                assertEquals(periodMicros, timer.timeout(), "events[" + iSignalled + "].timeout");
                assertEquals(repetition, timer.repetition(), "events[" + iSignalled + "].repetition");
                assertEquals(timerType, timer.timerType(), "events[" + iSignalled + "].timerType");
                assertEquals(contextId, timer.contextId(), "events[" + iSignalled + "].contextId");
            }
            final int iCancelled = 1 + PERIODIC_REPETITIONS;
            final Event cancelled = evts.get(iCancelled);
            timer.wrap(cancelled.payload(), 0);
            assertEquals(TimerEvents.TIMER_CANCELLED, cancelled.payloadType(), "events[" + iCancelled + "].type");
            assertEquals(timerId, timer.timerId(), "events[" + iCancelled + "].timerId");
            assertEquals(PERIODIC_REPETITIONS, timer.repetition(), "events[" + iCancelled + "].repetition");
            assertEquals(timerType, timer.timerType(), "events[" + iCancelled + "].timerType");
            assertEquals(contextId, timer.contextId(), "events[" + iCancelled + "].contextId");
            assertEquals(periodMicros, timer.timeout(), "events[" + iCancelled + "].timeout");
        };
        asserter.accept("original", events);
        System.out.println(events.size() + " events asserted");

        if (persisted) {
            System.out.println("-----------------------------------------------------------");
            System.out.println("starting replay");

            //when
            final List<Event> replay = new ArrayList<>();
            try (final ElaraRunner runner = app.chronicleQueue(oneTimeInput(input), "periodic", replay::add)) {
                runner.join(1000);
            }

            //then
            asserter.accept("replay", replay);
            for (int i = 0; i < events.size(); i++) {
                assertEquals(events.get(i).eventTime(), replay.get(i).eventTime(), "events[" + i + "].time == replay[" + i + "].time");
            }
            System.out.println(replay.size() + " replay events asserted");
        }
    }

}