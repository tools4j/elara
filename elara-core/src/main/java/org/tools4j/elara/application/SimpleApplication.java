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
package org.tools4j.elara.application;

import static java.util.Objects.requireNonNull;

public class SimpleApplication implements Application {

    private final String name;
    private final CommandProcessor commandProcessor;
    private final EventApplier eventApplier;

    public SimpleApplication(final String name,
                             final CommandProcessor commandProcessor,
                             final EventApplier eventApplier) {
        this.name = requireNonNull(name);
        this.commandProcessor = requireNonNull(commandProcessor);
        this.eventApplier = requireNonNull(eventApplier);
    }

    public static <R,W> Application create(final String name,
                                           final CommandProcessor commandProcessor,
                                           final EventApplier eventApplier) {
        return new SimpleApplication(name, commandProcessor, eventApplier);
    }

    @Override
    public CommandProcessor commandProcessor() {
        return commandProcessor;
    }

    @Override
    public EventApplier eventApplier() {
        return eventApplier;
    }

    @Override
    public String toString() {
        return name;
    }
}
