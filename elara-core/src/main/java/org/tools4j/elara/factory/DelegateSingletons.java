/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.agrona.concurrent.Agent;
import org.tools4j.elara.application.CommandProcessor;
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.OutputHandler;
import org.tools4j.elara.input.Input;
import org.tools4j.elara.output.Output;
import org.tools4j.elara.plugin.api.Plugin.Configuration;
import org.tools4j.elara.plugin.base.BaseState.Mutable;
import org.tools4j.elara.send.CommandSender;
import org.tools4j.elara.send.SenderSupplier;
import org.tools4j.elara.step.AgentStep;

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
    public SenderSupplier senderSupplier() {
        return delegate.senderSupplier();
    }

    @Override
    public Input[] inputs() {
        return delegate.inputs();
    }

    @Override
    public AgentStep sequencerStep() {
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
    public AgentStep commandPollerStep() {
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
    public AgentStep eventPollerStep() {
        return delegate.eventPollerStep();
    }

    @Override
    public Output output() {
        return delegate.output();
    }

    @Override
    public CommandSender loopbackCommandSender() {
        return delegate.loopbackCommandSender();
    }

    @Override
    public OutputHandler outputHandler() {
        return delegate.outputHandler();
    }

    @Override
    public AgentStep publisherStep() {
        return delegate.publisherStep();
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
    public Agent elaraAgent() {
        return delegate.elaraAgent();
    }

    @Override
    public AgentStep extraStepAlwaysWhenEventsApplied() {
        return delegate.extraStepAlwaysWhenEventsApplied();
    }

    @Override
    public AgentStep extraStepAlways() {
        return delegate.extraStepAlways();
    }
}
