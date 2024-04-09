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
package org.tools4j.elara.app.factory;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.EventReceiverConfig;
import org.tools4j.elara.app.state.MutableEventProcessingState;
import org.tools4j.elara.handler.DefaultPlaybackHandler;
import org.tools4j.elara.handler.PlaybackHandler;
import org.tools4j.elara.step.AgentStep;
import org.tools4j.elara.step.PlaybackFrameReceiverStep;
import org.tools4j.elara.time.MutableTimeSource;
import org.tools4j.elara.time.TimeSource;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class DefaultEventSubscriberFactory implements EventSubscriberFactory {

    private final AppConfig appConfig;
    private final EventReceiverConfig eventReceiverConfig;
    private final MutableEventProcessingState eventProcessingState;
    private final Supplier<? extends EventSubscriberFactory> eventSubscriberSingletons;
    private final Supplier<? extends EventProcessorFactory> eventProcessorSingletons;

    public DefaultEventSubscriberFactory(final AppConfig appConfig,
                                         final EventReceiverConfig eventReceiverConfig,
                                         final MutableEventProcessingState eventProcessingState,
                                         final Supplier<? extends EventSubscriberFactory> eventSubscriberSingletons,
                                         final Supplier<? extends EventProcessorFactory> eventProcessorSingletons) {
        this.appConfig = requireNonNull(appConfig);
        this.eventReceiverConfig = requireNonNull(eventReceiverConfig);
        this.eventProcessingState = requireNonNull(eventProcessingState);
        this.eventSubscriberSingletons = requireNonNull(eventSubscriberSingletons);
        this.eventProcessorSingletons = requireNonNull(eventProcessorSingletons);
    }

    private MutableTimeSource timeSource() {
        final TimeSource timeSource = appConfig.timeSource();
        if (timeSource instanceof MutableTimeSource) {
            return (MutableTimeSource)timeSource;
        }
        //this should not happen as the config validation should catch this earlier
        throw new IllegalStateException("Invalid time source in app config, must be mutable time source");
    }
    @Override
    public PlaybackHandler playbackHandler() {
        return new DefaultPlaybackHandler(
                timeSource(), eventProcessingState, eventProcessorSingletons.get().eventHandler()
        );
    }

    @Override
    public AgentStep playbackPollerStep() {
        return new PlaybackFrameReceiverStep(
                eventReceiverConfig.eventReceiver(),
                eventSubscriberSingletons.get().playbackHandler()
        );
    }
}
