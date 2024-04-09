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
package org.tools4j.elara.exception;

import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.app.message.Event;

/**
 * Duplicate commands and events are detected and skipped by the engine.  This handler allows applications to react to
 * such situations for instance by logging an information or warning message.
 */
@FunctionalInterface
public interface DuplicateHandler {

    /**
     * Command processing is skipped as all events for this command ID have already been applied.
     * <p>
     * This usually happens when<ol>
     *     <li>events from the event store are reapplied to reconstruct the application state</li>
     *     <li>subsequently commands for the same events are processed for instance by replaying the command store</li>
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
     *     <li>subsequently the same events are polled from the event store and attempted to reapply</li>
     * </ol>
     *
     * @param event the skipped event
     */
    default void skipEventApplying(final Event event) {
        //default is no-op;  apps may want to log something
    }

    /**
     * Event processing is skipped because it has already been applied.
     *
     * @param event the skipped event
     */
    default void skipEventProcessing(final Event event) {
        //default is no-op;  apps may want to log something
    }

    static DuplicateHandler systemDefault() {
        return ExceptionLogger.DEFAULT;
    }

    /** No-op handler silently ignoring all duplicates.*/
    DuplicateHandler NOOP = command -> {};

}
