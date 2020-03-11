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

import org.agrona.concurrent.IdleStrategy;
import org.tools4j.elara.application.Application;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.log.PeekableMessageLog;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.Plugin;
import org.tools4j.elara.time.TimeSource;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

public interface Context<A extends Application> {
    A application();

    List<Input> inputs();
    Context<A> input(Input input);
    Context<A> input(int id, Input.Poller poller);

    Output output();
    Context<A> output(Output output);

    PeekableMessageLog<Command> commandLog();
    Context<A> commandLog(String file);
    Context<A> commandLog(PeekableMessageLog<Command> commandLog);

    MessageLog<Event> eventLog();
    Context<A> eventLog(String file);
    Context<A> eventLog(MessageLog<Event> eventLog);

    Context<A> plugin(Plugin<?> plugin);
    Context<A> plugin(Plugin.Builder<? super A> plugin);
    <P> Context<A> plugin(Plugin<P> plugin, Function<? super A, ? extends P> pluginStateProvider);
    List<Plugin.Builder<? super A>> plugins();

    TimeSource timeSource();
    Context<A> timeSource(TimeSource timeSource);

    ExceptionHandler exceptionHandler();
    Context<A> exceptionHandler(ExceptionHandler exceptionHandler);

    DuplicateHandler duplicateHandler();
    Context<A> duplicateHandler(DuplicateHandler duplicateHandler);

    IdleStrategy idleStrategy();
    Context<A> idleStrategy(IdleStrategy idleStrategy);

    ThreadFactory threadFactory();
    Context<A> threadFactory(String threadName);
    Context<A> threadFactory(ThreadFactory threadFactory);

    Context<A> validateAndPopulateDefaults();

    static <A extends Application> Context<A> create(final A application) {
        return new DefaultContext<A>(application);
    }
}
