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
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.DuplicateHandler;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.application.ExceptionHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.time.TimeSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

final class DefaultContext implements Context {

    private static final String DEFAULT_THREAD_NAME = "elara";

    private CommandProcessor commandProcessor;
    private EventApplier eventApplier;
    private final List<Input> inputs = new ArrayList<>();
    private Output output = Output.NOOP;
    private MessageLog commandLog;
    private MessageLog eventLog;
    private TimeSource timeSource;
    private ExceptionHandler exceptionHandler = ExceptionHandler.DEFAULT;
    private DuplicateHandler duplicateHandler = DuplicateHandler.DEFAULT;
    private IdleStrategy idleStrategy = new BackoffIdleStrategy(
            100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));
    private ThreadFactory threadFactory;
    private final PluginContext plugins = new DefaultPluginContext(this);

    @Override
    public CommandProcessor commandProcessor() {
        return commandProcessor;
    }

    @Override
    public Context commandProcessor(final CommandProcessor commandProcessor) {
        this.commandProcessor = requireNonNull(commandProcessor);
        return this;
    }

    @Override
    public EventApplier eventApplier() {
        return eventApplier;
    }

    @Override
    public Context eventApplier(final EventApplier eventApplier) {
        this.eventApplier = requireNonNull(eventApplier);
        return this;
    }

    @Override
    public List<Input> inputs() {
        return inputs;
    }

    @Override
    public Context input(final Input input) {
        if (input.id() == Input.LOOPBACK_ID) {
            throw new IllegalStateException("Input id " + Input.LOOPBACK_ID + " is reserved for command loopback");
        }
        inputs.add(input);
        return this;
    }

    @Override
    public Context input(final int id, final Input.Poller poller) {
        return input(Input.create(id, poller));
    }

    @Override
    public Output output() {
        return output;
    }

    @Override
    public Context output(final Output output) {
        this.output = requireNonNull(output);
        return this;
    }

    @Override
    public MessageLog commandLog() {
        return commandLog;
    }

    @Override
    public Context commandLog(final String file) {
        throw new IllegalStateException("not supported yet");
    }

    @Override
    public Context commandLog(final MessageLog commandLog) {
        this.commandLog = requireNonNull(commandLog);
        return this;
    }

    @Override
    public MessageLog eventLog() {
        return eventLog;
    }

    @Override
    public Context eventLog(final String file) {
        throw new IllegalStateException("not supported yet");
    }

    @Override
    public Context eventLog(final MessageLog eventLog) {
        this.eventLog = requireNonNull(eventLog);
        return this;
    }

    @Override
    public TimeSource timeSource() {
        return timeSource;
    }

    @Override
    public Context timeSource(final TimeSource timeSource) {
        this.timeSource = timeSource;//null allowed here
        return this;
    }

    @Override
    public ExceptionHandler exceptionHandler() {
        return exceptionHandler;
    }

    @Override
    public Context exceptionHandler(final ExceptionHandler exceptionHandler) {
        this.exceptionHandler = requireNonNull(exceptionHandler);
        return this;
    }

    @Override
    public DuplicateHandler duplicateHandler() {
        return duplicateHandler;
    }

    @Override
    public Context duplicateHandler(final DuplicateHandler duplicateHandler) {
        this.duplicateHandler = requireNonNull(duplicateHandler);
        return this;
    }

    @Override
    public IdleStrategy idleStrategy() {
        return idleStrategy;
    }

    @Override
    public Context idleStrategy(final IdleStrategy idleStrategy) {
        this.idleStrategy = requireNonNull(idleStrategy);
        return this;
    }

    @Override
    public ThreadFactory threadFactory() {
        return threadFactory;
    }

    @Override
    public Context threadFactory(final ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;//null allowed here
        return this;
    }

    @Override
    public Context threadFactory(final String threadName) {
        return threadFactory(threadName == null ? null : r -> new Thread(null, r, threadName));
    }

    @Override
    public PluginContext plugins() {
        return plugins;
    }

    @Override
    public Context validateAndPopulateDefaults() {
        for (int i = 0; i < inputs.size(); i++) {
            for (int j = i + 1; j < inputs.size(); j++) {
                if (inputs.get(i).id() == inputs.get(j).id()) {
                    throw new IllegalStateException("Duplicate input id: " + inputs.get(i).id());
                }
            }
        }
        if (commandProcessor == null) {
            throw new IllegalStateException("Command processor must be set");
        }
        if (eventApplier == null) {
            throw new IllegalStateException("Event applier must be set");
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

}
