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
package org.tools4j.elara.event;

/**
 * State impact indicator returned by{@link EventRouter#rollbackAfterProcessing(RollbackMode)}.
 */
public enum StateImpact {
    /**
     * The application state is safe and not affected by the rollback.  This can be guaranteed only if no rolled-back
     * events have been passed to the {@link org.tools4j.elara.application.EventApplier event applier} yet.
     */
    STATE_UNAFFECTED,
    /**
     * The application state is possibly corrupted since some of the events rolled back have already been passed to the
     * {@link org.tools4j.elara.application.EventApplier event applier}.  The application state is safe only if either
     * <ul>
     *     <li>all events passed to the event applier did not modify application state, or</li>
     *     <li>undo events were routed prior to the rollback to revert the application state to the original values</li>
     * </ul>
     */
    STATE_CORRUPTION_POSSIBLE;
}
