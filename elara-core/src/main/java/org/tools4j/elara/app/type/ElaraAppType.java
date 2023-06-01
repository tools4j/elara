/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.type;

import org.tools4j.elara.app.config.AppConfig;
import org.tools4j.elara.app.config.CommandPollingMode;
import org.tools4j.elara.app.config.CommandStoreConfig;

/**
 * Defines standard Elara application types.
 * <p>
 * <img src="https://raw.githubusercontent.com/tools4j/elara/master/doc/elara-apps.jpg" alt="Elara Application Types">
 * <p>
 * <img src="https://raw.githubusercontent.com/tools4j/elara/master/doc/elara-sequencer-apps.jpg" alt="Elara Sequencer &amp; Processor Application Types">
 */
public enum ElaraAppType implements AppType {
    AllInOneApp,
    CoreApp,
    SequencerApp,
    PollerProcessorApp,
    SequencerProcessorApp,
    PassthroughApp,
    FeedbackApp,
    PlaybackApp,
    PublisherApp;

    @Override
    public boolean isSequencerApp() {
        switch (this) {
            case AllInOneApp:
            case CoreApp:
            case SequencerApp:
            case SequencerProcessorApp:
            case PassthroughApp:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isProcessorApp() {
        switch (this) {
            case AllInOneApp:
            case CoreApp:
            case PollerProcessorApp:
            case SequencerProcessorApp:
            case PassthroughApp:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isFeedbackApp() {
        return this == FeedbackApp;
    }

    @Override
    public boolean hasCommandStore(final AppConfig appConfig) {
        return isSequencerApp() && this != PassthroughApp &&
                appConfig instanceof CommandStoreConfig &&
                ((CommandStoreConfig)appConfig).commandPollingMode() != CommandPollingMode.NO_STORE;
    }

    @Override
    public boolean hasEventStore() {
        return isProcessorApp() || this == PlaybackApp;
    }
}
