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
package org.tools4j.elara.composite;

import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.route.EventRouter;

import static java.util.Objects.requireNonNull;

public class CompositeCommandProcessor implements CommandProcessor {

    private final CommandProcessor[] processors;

    private CompositeCommandProcessor(final CommandProcessor... processors) {
        this.processors = requireNonNull(processors);
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        for (final CommandProcessor processor : processors) {
            processor.onCommand(command, router);
        }
    }

    public static CommandProcessor create(final CommandProcessor... processors) {
        return Composites.composite(processors, NOOP, CommandProcessor[]::new, CompositeCommandProcessor::new);
    }
}
