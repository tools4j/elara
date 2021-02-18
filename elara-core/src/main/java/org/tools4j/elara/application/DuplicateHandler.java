/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.logging.ElaraLogger;
import org.tools4j.elara.logging.Logger;
import org.tools4j.elara.logging.Logger.Level;

import static org.tools4j.elara.logging.OutputStreamLogger.SYSTEM_FACTORY;

/**
 * Duplicate commands and events are detected and skipped by the engine.  This handler allows applications to log react
 * in such situations for instance to log an information or warning message.
 */
@FunctionalInterface
public interface DuplicateHandler {

    /**
     * Command processing is skipped as all events for this command ID have already been applied.
     * <p>
     * This usually happens when<ol>
     *     <li>events from the event log are reapplied to reconstruct the application state</li>
     *     <li>subsequently commands for the same events are processed for instance by replaying the command log</li>
     * </ol>
     *
     * @param command the skipped command
     */
    void skipCommandProcessing(Command command);

    /**
     * Event applying is skipped because it has already been applied.
     * <p>
     * This usually happens when <ol>
     *     <li>events are directly applied in the same run when commands are processed</li>
     *     <li>subsequently the same events are polled from the event log and attempted to reapply</li>
     * </ol>
     *
     * @param event the skipped event
     */
    default void skipEventApplying(final Event event) {
        //default is no-op;  apps may want to log something
    }

    /**
     * Handler logging skipped commands on INFO and skipped events on DEBUG
     * @param loggerFactory factory to create logger for duplicate handler class
     * @return a new duplicate handler instance
     */
    static DuplicateHandler loggingHandler(final Logger.Factory loggerFactory) {
        final ElaraLogger logger = ElaraLogger.create(loggerFactory, DuplicateHandler.class);
        return new DuplicateHandler() {
            @Override
            public void skipCommandProcessing(final Command command) {
                if (logger.isEnabled(Level.INFO)) {
                    logger.info("Skipping command: {}").replace(command).format();
                }
            }
            @Override
            public void skipEventApplying(final Event event) {
                if (logger.isEnabled(Level.DEBUG)) {
                    logger.debug("Skipping event: {}").replace(event).format();
                }
            }
        };
    }

    /** Default handler logging skipped commands on INFO and skipped events on DEBUG */
    DuplicateHandler DEFAULT = loggingHandler(SYSTEM_FACTORY);

    /** No-op handler silently ignoring all duplicates.*/
    DuplicateHandler NOOP = command -> {};

}
