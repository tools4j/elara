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
import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.command.DefaultCommandLoopback;
import org.tools4j.elara.event.AdminEventApplier;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.event.FlyweightEventRouter;
import org.tools4j.elara.handler.CommandHandler;
import org.tools4j.elara.handler.DedupEventHandler;
import org.tools4j.elara.handler.EventHandler;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.input.SimpleSequenceGenerator;
import org.tools4j.elara.log.ForwardingAppender;
import org.tools4j.elara.log.MessageLog;
import org.tools4j.elara.state.*;

final class Singletons {
    Singletons(final Context context) {
        final Application application = context.application();
        adminSequenceGenerator = new SimpleSequenceGenerator();
        serverState = new DefaultServerState();
        eventApplicationState = new DefaultEventApplicationState();
        commandLoopback = new DefaultCommandLoopback(
                context.commandLog().appender(),
                context.timeSource(),
                adminSequenceGenerator
        );
        adminEventApplier = new AdminEventApplier(new DefaultAdminStateProvider(
            new SimpleTimerState(1, new SimpleSequenceGenerator()),
            eventApplicationState
        ));
        eventHandler = new DedupEventHandler(
                eventApplicationState,
                new EventHandler(
                    commandLoopback,
                    context.output(),
                    (event, loopback) -> {
                        adminEventApplier.onEvent(event, loopback);
                        application.eventApplier().onEvent(event, loopback);
                    },
                    context.exceptionHandler()
                )
        );
        eventRouter = new FlyweightEventRouter(new ForwardingAppender<>(
                context.eventLog().appender(), eventHandler
        ));
        commandHandler = new CommandHandler(
                eventRouter,
                application.commandProcessor(),
                context.exceptionHandler()
        );
    }
    final SequenceGenerator adminSequenceGenerator;
    final ServerState serverState;
    final EventApplicationState.Mutable eventApplicationState;
    final CommandLoopback commandLoopback;
    final EventApplier adminEventApplier;
    final FlyweightEventRouter eventRouter;
    final CommandHandler commandHandler;
    final MessageLog.Handler<Event> eventHandler;
}
