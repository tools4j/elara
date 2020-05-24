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
//import org.agrona.DirectBuffer;
//import org.junit.jupiter.api.Test;
//import org.tools4j.elara.event.Event;
//import org.tools4j.elara.plugin.timer.TimerEvents;
//import org.tools4j.elara.run.ElaraRunner;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Queue;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.function.Consumer;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.tools4j.elara.plugin.timer.TimerEvents.timerId;
//import static org.tools4j.elara.plugin.timer.TimerEvents.timerTimeout;
//import static org.tools4j.elara.plugin.timer.TimerEvents.timerType;
//import static org.tools4j.elara.samples.timer.TimerApplication.PERIODIC_REPETITIONS;
//import static org.tools4j.elara.samples.timer.TimerApplication.TIMER_TYPE_PERIODIC;
//import static org.tools4j.elara.samples.timer.TimerApplication.TIMER_TYPE_SINGLE;
//
//class TimerApplicationTest {
//
//    @Test
//    public void singleTimers() {
//        singleTimers(false);
//    }
//
//    @Test
//    public void singleTimersPersisted() {
//        singleTimers(true);
//    }
//
//    private void singleTimers(final boolean persisted) {
//        //given
//        final int[] timeouts = {200, 500, 800, 1000};
//        final TimerApplication app = new TimerApplication();
//        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
//        commands.add(TimerApplication.startTimer(1001, timeouts[0]));
//        commands.add(TimerApplication.startTimer(1002, timeouts[1]));
//        commands.add(TimerApplication.startTimer(1003, timeouts[2]));
//        commands.add(TimerApplication.startTimer(1004, timeouts[3]));
//        final Queue<DirectBuffer> cmdClone = new ConcurrentLinkedQueue<>(commands);
//        final List<Event> events = new ArrayList<>();
//
//        //when
//        try (final ElaraRunner runner = persisted ?
//                app.chronicleQueue(commands, "single", events::add) :
//                app.inMemory(commands, events::add)) {
//            while (!commands.isEmpty()) {
//                runner.join(20);
//            }
//            runner.join(3000);
//        }
//
//        //then
//        final Consumer<List<Event>> asserter = evts -> {
//            assertEquals(8, evts.size(), "events.size");
//            for (int i = 0; i < timeouts.length; i++) {
//                final int se = i;
//                final int te = i + 4;
//                final Event started = evts.get(se);
//                final Event trigger = evts.get(te);
//                assertEquals(TimerEvents.TIMER_STARTED, started.type(), "events[" + se + "].type");
//                assertEquals(1000 + (i + 1), timerId(started), "events[" + se + "].timerId");
//                assertEquals(TIMER_TYPE_SINGLE, timerType(started), "events[" + se + "].timerType");
//                assertEquals(timeouts[i], timerTimeout(started), "events[" + se + "].timerTimeout");
//
//                assertEquals(TimerEvents.TIMER_EXPIRED, trigger.type(), "events[" + te + "].type");
//                assertEquals(1000 + (i + 1), timerId(trigger), "events[" + te + "].timerId");
//                assertEquals(TIMER_TYPE_SINGLE, timerType(trigger), "events[" + te + "].timerType");
//                assertEquals(timeouts[i], timerTimeout(trigger), "events[" + te + "].timerTimeout");
//            }
//        };
//        asserter.accept(events);
//        System.out.println(events.size() + " events asserted");
//
//        if (persisted) {
//            //when
//            final List<Event> replay = new ArrayList<>();
//            try (final ElaraRunner runner = app.chronicleQueue(cmdClone, "single", replay::add)) {
//                while (!cmdClone.isEmpty()) {
//                    runner.join(20);
//                }
//                runner.join(100);
//            }
//
//            //then
//            asserter.accept(replay);
//            for (int i = 0; i < events.size(); i++) {
//                assertEquals(events.get(i).time(), replay.get(i).time(), "events[" + i + "].time == replay[" + i + "].time");
//            }
//            System.out.println(replay.size() + " replay events asserted");
//        }
//    }
//
//    @Test
//    public void periodicTimer() {
//        periodicTimer(false);
//    }
//
//    @Test
//    public void periodicTimerPersisted() {
//        periodicTimer(true);
//    }
//
//    private void periodicTimer(final boolean persisted) {
//        //given
//        final int timerId = 666666666;
//        final long periodMillis = 500;
//        final TimerApplication app = new TimerApplication();
//        final Queue<DirectBuffer> commands = new ConcurrentLinkedQueue<>();
//        final List<Event> events = new ArrayList<>();
//
//        //when
//        commands.add(TimerApplication.startPeriodic(timerId, periodMillis));
//        try (final ElaraRunner runner = persisted ?
//                app.chronicleQueue(commands, "periodic", events::add) :
//                app.inMemory(commands, events::add)) {
//
//            //then
//            runner.join(100 + periodMillis * (PERIODIC_REPETITIONS + 1));
//        }
//
//        //then
//        final Consumer<List<Event>> asserter = evts -> {
//            assertEquals(2 * PERIODIC_REPETITIONS, evts.size(), "events.size");
//            for (int i = 0; i < PERIODIC_REPETITIONS; i++) {
//                final int se = 2*i;
//                final int ee = se + 1;
//                final Event started = evts.get(se);
//                final Event expired = evts.get(ee);
//                assertEquals(TimerEvents.TIMER_STARTED, started.type(), "events[" + se + "].type");
//                assertEquals(timerId, timerId(started), "events[" + se + "].timerId");
//                assertEquals(TIMER_TYPE_PERIODIC, timerType(started), "events[" + se + "].timerType");
//                assertEquals(periodMillis, timerTimeout(started), "events[" + se + "].timerTimeout");
//
//                assertEquals(TimerEvents.TIMER_EXPIRED, expired.type(), "events[" + ee + "].type");
//                assertEquals(timerId, timerId(expired), "events[" + ee + "].timerId");
//                assertEquals(TIMER_TYPE_PERIODIC, timerType(expired), "events[" + ee + "].timerType");
//                assertEquals(periodMillis, timerTimeout(expired), "events[" + ee + "].timerTimeout");
//            }
//        };
//        asserter.accept(events);
//        System.out.println(events.size() + " events asserted");
//
//        if (persisted) {
//            //when
//            final List<Event> replay = new ArrayList<>();
//            try (final ElaraRunner runner = app.chronicleQueue(commands, "periodic", replay::add)) {
//                while (!commands.isEmpty()) {
//                    runner.join(20);
//                }
//                runner.join(100);
//            }
//
//            //then
//            asserter.accept(replay);
//            for (int i = 0; i < events.size(); i++) {
//                assertEquals(events.get(i).time(), replay.get(i).time(), "events[" + i + "].time == replay[" + i + "].time");
//            }
//            System.out.println(replay.size() + " replay events asserted");
//        }
//    }
//
//}