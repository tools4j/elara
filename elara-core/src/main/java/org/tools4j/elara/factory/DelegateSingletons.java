/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.factory;

import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin.Configuration;
import org.tools4j.elara.plugin.base.BaseState.Mutable;
import org.tools4j.nobark.loop.LoopCondition;
import org.tools4j.nobark.loop.Step;

import static java.util.Objects.requireNonNull;

public class DelegateSingletons implements Singletons {

    private final Singletons delegate;

    public DelegateSingletons(final Singletons delegate) {
        this.delegate = requireNonNull(delegate);
    }

    protected Singletons singletons() {
        return delegate;
    }

    @Override
    public Receiver receiver() {
        return delegate.receiver();
    }

    @Override
    public Input[] inputs() {
        return delegate.inputs();
    }

    @Override
    public Step sequencerStep() {
        return delegate.sequencerStep();
    }

    @Override
    public CommandProcessor commandProcessor() {
        return delegate.commandProcessor();
    }

    @Override
    public CommandHandler commandHandler() {
        return delegate.commandHandler();
    }

    @Override
    public Step commandPollerStep() {
        return delegate.commandPollerStep();
    }

    @Override
    public EventApplier eventApplier() {
        return delegate.eventApplier();
    }

    @Override
    public EventHandler eventHandler() {
        return delegate.eventHandler();
    }

    @Override
    public Step eventPollerStep() {
        return delegate.eventPollerStep();
    }

    @Override
    public Output output() {
        return delegate.output();
    }

    @Override
    public SequenceGenerator loopbackSequenceGenerator() {
        return delegate.loopbackSequenceGenerator();
    }

    @Override
    public CommandLoopback commandLoopback() {
        return delegate.commandLoopback();
    }

    @Override
    public OutputHandler outputHandler() {
        return delegate.outputHandler();
    }

    @Override
    public Step outputStep() {
        return delegate.outputStep();
    }

    @Override
    public Mutable baseState() {
        return delegate.baseState();
    }

    @Override
    public Configuration[] plugins() {
        return delegate.plugins();
    }

    @Override
    public Runnable initStep() {
        return delegate.initStep();
    }

    @Override
    public LoopCondition runningCondition() {
        return delegate.runningCondition();
    }

    @Override
    public Step dutyCycleStep() {
        return delegate.dutyCycleStep();
    }

    @Override
    public Step dutyCycleExtraStep() {
        return delegate.dutyCycleExtraStep();
    }

    @Override
    public Step[] dutyCycleWithExtraSteps() {
        return delegate.dutyCycleWithExtraSteps();
    }
}
