/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.log;

import org.tools4j.elara.log.MessageLog.Poller;

/**
 * A poller tracking (gap free) indexes of entries in the message log.  Note that all methods that are moving by more
 * than a single index are slow!
 */
public interface IndexTrackingPoller extends Poller {
    /**
     * Returns the current index of this poller.
     * <p>
     * It returns the index of the message currently polled if invoked while polling a message.
     * Otherwise it returns the index of the next message to be polled.
     * <p>
     * The index monotonically increasing and gap free.
     *
     * @return the current index of this poller.
     *
     */
    long index();

    /**
     * NOTE: this method is possibly slow if it has to move a lot of positions!
     *
     * Moves to the specified index
     * @param index the index to move to, valid from zero to (#entries - 1)
     * @return true if such an index is found
     */
    boolean moveToIndex(long index);

    /**
     * NOTE: this method is possibly slow if it has to move a lot of positions!
     *
     * Moves to the end of the message log.
     * @return this poller
     */
    @Override
    Poller moveToEnd();

    static IndexTrackingPoller create(final MessageLog messageLog) {
        return new DefaultIndexTrackingPoller(messageLog);
    }
}
