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

import static java.util.Objects.requireNonNull;

public interface Interceptor {

    Interceptor NOOP = new Interceptor() {};

    default AppFactory interceptOrNull(final AppFactory original) {
        return null;
    }

    default AgentStepFactory interceptOrNull(final AgentStepFactory original) {
        return null;
    }

    default SequencerFactory interceptOrNull(final SequencerFactory original) {
        return null;
    }

    default CommandPollerFactory interceptOrNull(final CommandPollerFactory original) {
        return null;
    }

    default ProcessorFactory interceptOrNull(final ProcessorFactory original) {
        return null;
    }

    default InOutFactory interceptOrNull(final InOutFactory original) {
        return null;
    }

    default PublisherFactory interceptOrNull(final PublisherFactory original) {
        return null;
    }

    default CommandStreamFactory interceptOrNull(final CommandStreamFactory original) {
        return null;
    }

    default EventStreamFactory interceptOrNull(final EventStreamFactory original) {
        return null;
    }

    default Interceptor andThen(final Interceptor next) {
        requireNonNull(next);
        if (this == NOOP) {
            return next;
        }
        if (next == NOOP) {
            return this;
        }
        final Interceptor first = this;
        return new Interceptor() {
            @Override
            public AppFactory interceptOrNull(final AppFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public AgentStepFactory interceptOrNull(final AgentStepFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public SequencerFactory interceptOrNull(final SequencerFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public CommandPollerFactory interceptOrNull(final CommandPollerFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public ProcessorFactory interceptOrNull(final ProcessorFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public InOutFactory interceptOrNull(final InOutFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public PublisherFactory interceptOrNull(final PublisherFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public CommandStreamFactory interceptOrNull(final CommandStreamFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }

            @Override
            public EventStreamFactory interceptOrNull(final EventStreamFactory original) {
                return next.interceptOrNull(first.interceptOrNull(original));
            }
        };
    }
}
