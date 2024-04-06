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
package org.tools4j.elara.source;

import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.send.CommandSender;

/**
 * The command context provides access to all {@link #commandSources() command sources}.  It also provides an aggregated
 * view of {@link #hasInFlightCommand() in-flight} commands from all sources -- commands sent whose corresponding event
 * (or events) have not been received back yet.
 */
public interface CommandContext {
    /**
     * True if commands from any source are currently in-flight, that is, if at least one command has been sent whose
     * commit event was not yet processed.  This may be useful to defer event processing until all events have from sent
     * commands have been received back and the application state updated.
     * <p>
     * Note that {@link CommandProcessor#onCommand(Command, EventRouter) command processing}
     * can yield more than one event. The commit event is the last event that corresponds to the same command.
     * <p>
     * This method returns false if no commands are currently in-flight, or if the currently processed event is the
     * commit event of the last in-flight command.
     * <p>
     * This method provides an aggregated view over all command sources and returns true if any of the
     * {@link #commandSources() command sources} has a command in-flight. The in-flight state of an individual command
     * source is available through {@link CommandSource#hasInFlightCommand()} and the source can for instance be
     * accessed via {@link CommandSender#source()}.
     *
     * @return true if at least one command source has a command in-flight
     * @see CommandSource#hasInFlightCommand()
     * @see CommandSender#source()
     */
    boolean hasInFlightCommand();

    /**
     * Returns access to all command sources used by this application.
     * @return the provider for command sources used by this application.
     */
    CommandSourceProvider commandSources();
}
