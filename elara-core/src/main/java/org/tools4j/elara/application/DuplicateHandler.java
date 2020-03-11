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

import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;

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
    default void skipCommandProcessing(final Command command) {
        //default is no-op;  apps may want to log something
    }

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
     * A command is dropped and not appended to the command log because it is already stored there.
     * <p>
     * This can happen when upstream applications re-send messages and those messages are identified as duplicates by
     * the sequencer.  Duplicates are identified via unique (input-id/sequence) pair of the command.  Duplicate
     * detection utilises the constraint that sequences must be monotonically increasing per input.
     *
     * @param command the dropped command
     */
    void dropCommandReceived(final Command command);

    /** Default handler printing only dropped commands to system output */
    DuplicateHandler DEFAULT = command -> {
        System.out.println("Dropping duplicate command: " + command);
    };

    /** No-op handler silently ignoring all duplicates.*/
    DuplicateHandler NOOP = command -> {};

}
