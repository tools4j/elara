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
package org.tools4j.elara.route;

/**
 * Skip mode indicator returned by {@link EventRouter#skipCommand(boolean)}.
 */
public enum SkipMode {
    /**
     * The command has not been skipped; routed events are applied to the application state.
     */
    NONE,
    /**
     * The command was skipped and the no events have been applied to the application state.  The only state change is
     * the confirmation that this command has now been processed which is applied in form of a
     * {@link org.tools4j.elara.event.EventType#COMMIT COMMIT} event.
     * <p>
     * Skipped outcome can only be guaranteed if no events have been routed yet when the command is skipped.  Events
     * routed after skipping the command will be ignored (this includes all events routed by plugin commands as they are
     * processed after application commands).
     */
    SKIPPED,
    /**
     * The command was skipped and the no events have been applied to the application state.  The application is in
     * exactly the same state as before processing this command.  Note that the command will also not be marked as
     * processed.  The next successfully processed command from the same input will fill the gap and act as a conflating
     * confirmation.  Multiple commands from the same input can be conflated before filling the gaps through the
     * processing of a command from the same input.
     * <p>
     * Conflation outcome can only be guaranteed if no events have been routed yet when the command is skipped.  Events
     * routed after skipping the command will be ignored (this includes all events routed by plugin commands as they are
     * processed after application commands).
     */
    CONFLATED,
    /**
     * The command was skipped but some events had already been applied to the application state when skipping the
     * command.  Events routed after consuming the command will be ignored (this includes all events routed by plugin
     * commands as they are processed after application commands).
     */
    CONSUMED
}
