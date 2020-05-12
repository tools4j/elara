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

import org.tools4j.elara.application.Application;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.DefaultCommandLoopback;
import org.tools4j.elara.route.FlyweightEventRouter;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.handler.ProcessingCommandHandler;

final class Singletons {
    <A extends Application> Singletons(final Context context, final Plugins plugins) {
        commandLoopback = new DefaultCommandLoopback(
                context.commandLog().appender(),
                context.timeSource(),
                plugins.adminSequenceGenerator
        );
        eventHandler = new EventHandler(
                plugins.baseState, commandLoopback,
                plugins.output,
                plugins.eventApplier,
                context.exceptionHandler(),
                context.duplicateHandler()
        );
        eventRouter = new FlyweightEventRouter(context.eventLog().appender(), eventHandler);
        commandHandler = new ProcessingCommandHandler(
                plugins.baseState, eventRouter,
                plugins.commandProcessor,
                context.exceptionHandler(),
                context.duplicateHandler()
        );
    }

    final CommandLoopback commandLoopback;
    final FlyweightEventRouter eventRouter;
    final ProcessingCommandHandler commandHandler;
    final EventHandler eventHandler;

 }
