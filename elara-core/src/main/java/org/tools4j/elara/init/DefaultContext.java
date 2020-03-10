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
package org.tools4j.elara.init;

import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.application.Application;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.handler.InputHandlerFactory;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.PeekableMessageLog;
import org.tools4j.elara.loop.CommandPollerStep;
import org.tools4j.elara.loop.DutyCycle;
import org.tools4j.elara.loop.EventApplierStep;
import org.tools4j.elara.loop.SequencerStep;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.Plugin;
import org.tools4j.elara.time.TimeSource;
import org.tools4j.nobark.run.ThreadLike;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class DefaultContext<A extends Application> implements Context<A> {

    private static final String DEFAULT_THREAD_NAME = "duty-cycle";

    private final A application;
    private final List<Input> inputs = new ArrayList<>();
    private final List<Plugin.Builder<? super A>> plugins = new ArrayList<>();
    private Output output = event -> {};
    private PeekableMessageLog<Command> commandLog;
    private MessageLog<Event> eventLog;
    private TimeSource timeSource;
    private ExceptionHandler exceptionHandler = ExceptionHandler.DEFAULT;
    private IdleStrategy idleStrategy = new BackoffIdleStrategy(
            100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
    private ThreadFactory threadFactory;

    public DefaultContext(final A application) {
        this.application = requireNonNull(application);
    }

    @Override
    public A application() {
        return application;
    }

    @Override
    public List<Input> inputs() {
        return inputs;
    }

    @Override
    public Context<A> input(final Input input) {
        inputs.add(input);
        return this;
    }

    @Override
    public Context<A> input(final int id, final Input.Poller poller) {
        return input(Input.create(id, poller));
    }

    @Override
    public Output output() {
        return output;
    }

    @Override
    public Context<A> output(final Output output) {
        this.output = requireNonNull(output);
        return this;
    }

    @Override
    public PeekableMessageLog<Command> commandLog() {
        return commandLog;
    }

    @Override
    public Context<A> commandLog(final String file) {
        throw new IllegalStateException("not supported yet");
    }

    @Override
    public Context<A> commandLog(final PeekableMessageLog<Command> commandLog) {
        this.commandLog = requireNonNull(commandLog);
        return this;
    }

    @Override
    public MessageLog<Event> eventLog() {
        return eventLog;
    }

    @Override
    public Context<A> eventLog(final String file) {
        throw new IllegalStateException("not supported yet");
    }

    @Override
    public Context<A> eventLog(final MessageLog<Event> eventLog) {
        this.eventLog = requireNonNull(eventLog);
        return this;
    }

    @Override
    public Context<A> plugin(final Plugin<?> plugin) {
        return plugin(plugin.builder());
    }

    @Override
    public <P> Context<A> plugin(final Plugin<P> plugin, final Function<? super A, ? extends P> pluginStateProvider) {
        return plugin(plugin.builder(pluginStateProvider));
    }

    @Override
    public Context<A> plugin(final Plugin.Builder<? super A> plugin) {
        plugins.add(plugin);
        return this;
    }

    @Override
    public List<Plugin.Builder<? super A>> plugins() {
        return plugins;
    }

    @Override
    public TimeSource timeSource() {
        return timeSource;
    }

    @Override
    public Context<A> timeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;//null allowed here
        return this;
    }

    @Override
    public ExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public Context<A> exceptionHandler(final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = requireNonNull(exceptionHandler);
        return this;
    }

    @Override
    public IdleStrategy idleStrategy() {
        return idleStrategy;
    }

    @Override
    public Context<A> idleStrategy(final IdleStrategy idleStrategy) {
        this.idleStrategy = requireNonNull(idleStrategy);
        return this;
    }

    @Override
    public ThreadFactory threadFactory() {
        return threadFactory;
    }

    @Override
    public Context<A> threadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;//null allowed here
        return this;
    }

    @Override
    public Context<A> threadFactory(final String threadName) {
        return threadFactory(threadName == null ? null : r -> new Thread(null, r, threadName));
    }

    @Override
    public Context<A> validateAndPopulateDefaults() {
        if (application == null) {
            throw new IllegalStateException("Application must be set");
        }
        for (int i = 0; i < inputs.size(); i++) {
            for (int j = i + 1; j < inputs.size(); j++) {
                if (inputs.get(i).id() == inputs.get(j).id()) {
                    throw new IllegalStateException("Duplicate input id: " + inputs.get(i).id());
                }
            }
        }
        if (commandLog == null) {
            throw new IllegalStateException("Command log must be set");
        }
        if (eventLog == null) {
            throw new IllegalStateException("Event log must be set");
        }
        if (timeSource == null) {
            timeSource = System::currentTimeMillis;
        }
        if (threadFactory == null) {
            threadFactory(DEFAULT_THREAD_NAME);
        }
        return this;
    }

    public static DutyCycle dutyCycle(final Context<?> context) {
        final Singletons singletons = new Singletons(context);
        return new DutyCycle(
                sequencerStep(context, singletons),
                commandPollerStep(context, singletons),
                eventApplierStep(context, singletons)
        );
    }

    static InputHandlerFactory inputHandlerFactory(final Context<?> context) {
        return new InputHandlerFactory(context.commandLog().appender(), context.timeSource());
    }

    static SequencerStep sequencerStep(final Context<?> context,
                                       final Singletons singletons) {
        return new SequencerStep(inputHandlerFactory(context), singletons.plugins.inputs);
    }

    static CommandPollerStep commandPollerStep(final Context<?> context,
                                               final Singletons singletons) {
        return new CommandPollerStep(context.commandLog().poller(), singletons.commandHandler);
    }

    static EventApplierStep eventApplierStep(final Context<?> context, final Singletons singletons) {
        return new EventApplierStep(
                context.eventLog().poller(),
                singletons.eventHandler
        );
    }

    static org.tools4j.nobark.loop.IdleStrategy idleStrategy(final Context<?> context) {
        final IdleStrategy idleStrategy = requireNonNull(context.idleStrategy());
        return new org.tools4j.nobark.loop.IdleStrategy() {
            @Override
            public void idle() {
                idleStrategy.idle();
            }

            @Override
            public void reset() {
                idleStrategy.reset();
            }

            @Override
            public void idle(final boolean workDone) {
                idleStrategy.idle(workDone ? 1 : 0);
            }

            @Override
            public String toString() {
                return idleStrategy.toString();
            }
        };
    }

    static ThreadLike start(final Context<?> context) {
        context.validateAndPopulateDefaults();
        return dutyCycle(context).start(
                idleStrategy(context),
                context.exceptionHandler(),
                context.threadFactory()
        );
    }

}
