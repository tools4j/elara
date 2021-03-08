/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.junit.jupiter.api.Test;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.timer.DeadlineHeapTimerState;
import org.tools4j.elara.plugin.timer.SimpleTimerState;
import org.tools4j.elara.plugin.timer.TimerEvents;
import org.tools4j.elara.plugin.timer.TimerState;
import org.tools4j.elara.run.ElaraRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerId;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerTimeout;
import static org.tools4j.elara.plugin.timer.TimerEvents.timerType;
import static org.tools4j.elara.samples.timer.TimerApplication.PERIODIC_REPETITIONS;
import static org.tools4j.elara.samples.timer.TimerApplication.TIMER_TYPE_PERIODIC;
import static org.tools4j.elara.samples.timer.TimerApplication.TIMER_TYPE_SINGLE;

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

    private void singleTimers(final boolean persisted, final boolean simpleState) {
        //given
        final int[] timeouts = {200, 500, 800, 1000};
        final TimerApplication app = new TimerApplication();
        final Supplier<? extends TimerState.Mutable> timerStateSupplier = simpleState ?
                SimpleTimerState::new : DeadlineHeapTimerState::new;
        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
        commands.add(TimerApplication.startTimer(1001, timeouts[0]));
        commands.add(TimerApplication.startTimer(1002, timeouts[1]));
        commands.add(TimerApplication.startTimer(1003, timeouts[2]));
        commands.add(TimerApplication.startTimer(1004, timeouts[3]));
        final Queue<DirectBuffer> cmdClone = new ConcurrentLinkedQueue<>(commands);
        final List<Event> events = new ArrayList<>();

        //when
        try (final ElaraRunner runner = persisted ?
                app.chronicleQueue(commands, "single", events::add) :
                app.inMemory(commands, events::add, timerStateSupplier)) {
            while (!commands.isEmpty()) {
                runner.join(20);
            }
            runner.join(3000);
        }

        //then
        final Consumer<List<Event>> asserter = evts -> {
            System.out.println("===========================================================");
            System.out.println("asserting events at " + Instant.now());
            assertEquals(8, evts.size(), "events.size");
            for (int i = 0; i < timeouts.length; i++) {
                final int iStarted = i;
                final int iExpired = i + 4;
                final Event started = evts.get(iStarted);
                final Event expired = evts.get(iExpired);
                assertEquals(TimerEvents.TIMER_STARTED, started.type(), "events[" + iStarted + "].type");
                assertEquals(1000 + (i + 1), timerId(started), "events[" + iStarted + "].timerId");
                assertEquals(TIMER_TYPE_SINGLE, timerType(started), "events[" + iStarted + "].timerType");
                assertEquals(timeouts[i], timerTimeout(started), "events[" + iStarted + "].timerTimeout");

                assertEquals(TimerEvents.TIMER_EXPIRED, expired.type(), "events[" + iExpired + "].type");
                assertEquals(1000 + (i + 1), timerId(expired), "events[" + iExpired + "].timerId");
                assertEquals(TIMER_TYPE_SINGLE, timerType(expired), "events[" + iExpired + "].timerType");
                assertEquals(timeouts[i], timerTimeout(expired), "events[" + iExpired + "].timerTimeout");
            }
        };
        asserter.accept(events);
        System.out.println(events.size() + " events asserted");

        if (persisted) {
            //when
            final List<Event> replay = new ArrayList<>();
            try (final ElaraRunner runner = app.chronicleQueue(cmdClone, "single", replay::add)) {
                while (!cmdClone.isEmpty()) {
                    runner.join(20);
                }
                runner.join(100);
            }

            //then
            asserter.accept(replay);
            for (int i = 0; i < events.size(); i++) {
                assertEquals(events.get(i).time(), replay.get(i).time(), "events[" + i + "].time == replay[" + i + "].time");
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
        final int timerId = 666666666;
        final long periodMillis = 500;
        final TimerApplication app = new TimerApplication();
        final Supplier<? extends TimerState.Mutable> timerStateSupplier = simpleState ?
                SimpleTimerState::new : DeadlineHeapTimerState::new;
        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
        final List<Event> events = new ArrayList<>();

        //when
        commands.add(TimerApplication.startPeriodic(timerId, periodMillis));
        try (final ElaraRunner runner = persisted ?
                app.chronicleQueue(commands, "periodic", events::add) :
                app.inMemory(commands, events::add, timerStateSupplier)) {

            //then
            runner.join(100 + periodMillis * (PERIODIC_REPETITIONS + 1));
        }

        //then
        final Consumer<List<Event>> asserter = evts -> {
            System.out.println("===========================================================");
            System.out.println("asserting events at " + Instant.now());
            assertEquals(PERIODIC_REPETITIONS + 2, evts.size(), "events.size");

            final int iStarted = 0;
            final Event started = evts.get(iStarted);
            assertEquals(TimerEvents.TIMER_STARTED, started.type(), "events[" + iStarted + "].type");
            assertEquals(timerId, timerId(started), "events[" + iStarted + "].timerId");
            assertEquals(TIMER_TYPE_PERIODIC, timerType(started), "events[" + iStarted + "].timerType");
            assertEquals(periodMillis, timerTimeout(started), "events[" + iStarted + "].timerTimeout");
            for (int i = 0; i < PERIODIC_REPETITIONS; i++) {
                final int iFired = i + 1;
                final Event fired = evts.get(iFired);
                assertEquals(TimerEvents.TIMER_FIRED, fired.type(), "events[" + iFired + "].type");
                assertEquals(timerId, timerId(fired), "events[" + iFired + "].timerId");
                assertEquals(TIMER_TYPE_PERIODIC, timerType(fired), "events[" + iFired + "].timerType");
                assertEquals(periodMillis, timerTimeout(fired), "events[" + iFired + "].timerTimeout");
            }
            final int iStopped = 1 + PERIODIC_REPETITIONS;
            final Event stopped = evts.get(iStopped);
            assertEquals(TimerEvents.TIMER_STOPPED, stopped.type(), "events[" + iStopped + "].type");
            assertEquals(timerId, timerId(stopped), "events[" + iStopped + "].timerId");
            assertEquals(TIMER_TYPE_PERIODIC, timerType(stopped), "events[" + iStopped + "].timerType");
            assertEquals(periodMillis, timerTimeout(stopped), "events[" + iStopped + "].timerTimeout");
        };
        asserter.accept(events);
        System.out.println(events.size() + " events asserted");

        if (persisted) {
            //when
            final List<Event> replay = new ArrayList<>();
            try (final ElaraRunner runner = app.chronicleQueue(commands, "periodic", replay::add)) {
                while (!commands.isEmpty()) {
                    runner.join(20);
                }
                runner.join(100);
            }

            //then
            asserter.accept(replay);
            for (int i = 0; i < events.size(); i++) {
                assertEquals(events.get(i).time(), replay.get(i).time(), "events[" + i + "].time == replay[" + i + "].time");
            }
            System.out.println(replay.size() + " replay events asserted");
        }
    }

}