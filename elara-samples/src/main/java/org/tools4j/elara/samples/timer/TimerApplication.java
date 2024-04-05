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
package org.tools4j.elara.samples.timer;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.wire.WireType;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.type.AllInOneAppConfig;
import org.tools4j.elara.chronicle.ChronicleMessageStore;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.flyweight.FlyweightEvent;
import org.tools4j.elara.format.TimeFormatter.MicroTimeFormatter;
import org.tools4j.elara.input.InputPoller;
import org.tools4j.elara.plugin.api.Plugins;
import org.tools4j.elara.plugin.timer.FlyweightTimerPayload;
import org.tools4j.elara.plugin.timer.MutableTimerState;
import org.tools4j.elara.plugin.timer.SimpleTimerState;
import org.tools4j.elara.plugin.timer.Timer.Style;
import org.tools4j.elara.plugin.timer.TimerCommands;
import org.tools4j.elara.plugin.timer.TimerController.ControlContext;
import org.tools4j.elara.plugin.timer.TimerEvents;
import org.tools4j.elara.plugin.timer.TimerPlugin;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.run.Elara;
import org.tools4j.elara.run.ElaraRunner;
import org.tools4j.elara.samples.time.PseudoMicroClock;
import org.tools4j.elara.store.InMemoryStore;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class TimerApplication {

    public static final int SOURCE_ID = 777;
    public static final int PERIODIC_REPETITIONS = 5;

    public final TimerPlugin timerPlugin = Plugins.timerPlugin();

    public ElaraRunner inMemory(final InputPoller inputPoller,
                                final Consumer<? super Event> eventConsumer) {
        return inMemory(inputPoller, eventConsumer, SimpleTimerState::new);
    }

    public ElaraRunner inMemory(final InputPoller inputPoller,
                                final Consumer<? super Event> eventConsumer,
                                final Supplier<? extends MutableTimerState> timerStateSupplier) {
        return Elara.launch(AllInOneAppConfig.configure()
                .commandProcessor(this::process)
                .eventApplier(eventApplier(eventConsumer))
                .input(SOURCE_ID, inputPoller)
                .commandStore(new InMemoryStore())
                .eventStore(new InMemoryStore())
                .timeSource(new PseudoMicroClock())
                .plugin(timerPlugin, timerStateSupplier)
                .populateDefaults()
        );
    }

    public ElaraRunner chronicleQueue(final InputPoller inputPoller,
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
        return Elara.launch(AllInOneAppConfig.configure()
                .commandProcessor(this::process)
                .eventApplier(eventApplier(eventConsumer))
                .input(SOURCE_ID, inputPoller)
                .commandStore(new ChronicleMessageStore(cq))
                .eventStore(new ChronicleMessageStore(eq))
                .timeSource(new PseudoMicroClock())
                .plugin(timerPlugin)
                .populateDefaults()
        );
    }

    private EventApplier eventApplier(final Consumer<? super Event> eventConsumer) {
        requireNonNull(eventConsumer);
        return event -> {
            apply(event);
            eventConsumer.accept(cloneEvent(event));
        };
    }


    private void process(final Command command, final EventRouter router) {
        System.out.println("-----------------------------------------------------------");
//        System.out.println("processing: " + command + ", payload=" + payloadFor(command.type(), command.payload()));
        if (TimerCommands.isTimerCommand(command)) {
            final String name = TimerCommands.timerCommandName(command);
            final FlyweightTimerPayload timer = new FlyweightTimerPayload().wrap(command.payload(), 0);
            System.out.println("...COMMAND: " + name + ": timer=" + timer + ", command=" + formatCommand(command));
            if (timer.style() == Style.PERIODIC && timer.repetition() > PERIODIC_REPETITIONS) {
                try (ControlContext timerControl = timerPlugin.controller()) {
                    timerControl.cancelTimer(timer.timerId());
                }
            }
        }
    }

    private void apply(final Event event) {
//        System.out.println("applied: " + event + ", payload=" + payloadFor(event.type(), event.payload()));
        if (TimerEvents.isTimerEvent(event)) {
            final String name = TimerEvents.timerEventName(event);
            final FlyweightTimerPayload timer = new FlyweightTimerPayload().wrap(event.payload(), 0);
            System.out.println("...EVENT: " + name + ": timer=" + timer + ", event=" + formatEvent(event));
        }
    }

    private static String formatCommand(final Command command) {
        return "Command{source-id=" + command.sourceId() + "|source-seq=" + command.sourceSequence() +
                "|payload-type=" + command.payloadType() + "|command-time=" + formatTime(command.commandTime()) + "}";
    }

    private static String formatEvent(final Event event) {
        return "Command{source-id=" + event.sourceId() + "|source-seq=" + event.sourceSequence() +
                "|event-seq=" + event.eventSequence() + "|event-type=" + event.eventType() +
                "|payload-type=" + event.payloadType() + "|event-time=" + formatTime(event.eventTime()) + "}";
    }

    private static String formatTime(final long time) {
        return MicroTimeFormatter.DEFAULT.formatTime(time);
    }

    private static Event cloneEvent(final Event event) {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
        event.writeTo(buffer, 0);
        return new FlyweightEvent().wrap(buffer, 0);
    }

    public static InputPoller oneTimeInput(final InputPoller inputPoller) {
        requireNonNull(inputPoller);
        final boolean[] inputPolled = {false};
        return context -> {
            if (!inputPolled[0]) {
                final int result = inputPoller.poll(context);
                inputPolled[0] = true;
                return result;
            }
            return 0;
        };
    }
}