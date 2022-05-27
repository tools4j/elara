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
package org.tools4j.elara.app.factory;

import org.agrona.concurrent.Agent;
import org.tools4j.elara.agent.AllInOneAgent;
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.type.AllInOneAppConfig;

public class AllInOneAppFactory implements AppFactory {

    private final SequencerFactory sequencerSingletons;
    private final ProcessorFactory processorSingletons;
    private final InOutFactory inOutSingletons;
    private final CommandPollerFactory commandPollerSingletons;
    private final PublisherFactory publisherSingletons;
    private final AgentStepFactory agentStepSingletons;
    private final PluginFactory pluginSingletons;

    public AllInOneAppFactory(final AllInOneAppConfig config) {
        this.sequencerSingletons = Singletons.create(
                config.commandPollingMode() == CommandPollingMode.NO_STORE ?
                        new ProcessingSequencerFactory(config, this::sequencerSingletons, this::processorSingletons, this::inOutSingletons) :
                        new AppendingSequencerFactory(config, config, this::sequencerSingletons, this::inOutSingletons)
                );
        this.processorSingletons = Singletons.create(new DefaultProcessorFactory(config, config, this::processorSingletons, this::pluginSingletons));
        this.inOutSingletons = Singletons.create(new DefaultInOutFactory(config, config, this::pluginSingletons));
        this.commandPollerSingletons = Singletons.create(new DefaultCommandPollerFactory(config, this::processorSingletons));
        this.publisherSingletons = Singletons.create(new DefaultPublisherFactory(config, config, this::publisherSingletons, this::sequencerSingletons, this::inOutSingletons));
        this.agentStepSingletons = Singletons.create(new DefaultAgentStepFactory(config, this::pluginSingletons));
        this.pluginSingletons = Singletons.create(new DefaultPluginFactory(
                config, config, this::pluginSingletons));
    }

    private ProcessorFactory processorSingletons() {
        return processorSingletons;
    }

    private InOutFactory inOutSingletons() {
        return inOutSingletons;
    }

    private SequencerFactory sequencerSingletons() {
        return sequencerSingletons;
    }

    private PublisherFactory publisherSingletons() {
        return publisherSingletons;
    }

    private PluginFactory pluginSingletons() {
        return pluginSingletons;
    }

    @Override
    public Agent agent() {
//        return new CoreAgent(
//                sequencerSingletons.sequencerStep(),
//                commandPollerSingletons.commandPollerStep(),
//                processorSingletons.eventPollerStep(),
//                agentStepSingletons.extraStepAlwaysWhenEventsApplied(),
//                agentStepSingletons.extraStepAlways());
        return new AllInOneAgent(
                sequencerSingletons.sequencerStep(),
                commandPollerSingletons.commandPollerStep(),
                processorSingletons.eventPollerStep(),
                publisherSingletons.publisherStep(),
                agentStepSingletons.extraStepAlwaysWhenEventsApplied(),
                agentStepSingletons.extraStepAlways());
    }
}
