/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.log.MessageLog.Handler;
import org.tools4j.elara.log.MessageLog.Handler.Result;
import org.tools4j.elara.log.MessageLog.Poller;

public class DefaultIndexTrackingPoller implements IndexTrackingPoller {

    private final Poller poller;
    private long index;

    public DefaultIndexTrackingPoller(final MessageLog messageLog) {
        this.poller = messageLog.poller();
        moveToStart();
    }

    @Override
    public long index() {
        return index;
    }

    @Override
    public long entryId() {
        return poller.entryId();
    }

    @Override
    public boolean moveTo(final long entryId) {
        if (poller.entryId() == entryId) {
            return true;
        }
        final long originalEntryId = poller.entryId();
        boolean found = false;
        try {
            poller.moveToStart();
            long index = 0;
            while (poller.entryId() != entryId) {
                if (poller.poll(message -> Result.POLL) == 0) {
                    break;
                }
                index++;
            }
            if (poller.entryId() == entryId) {
                this.index = index;
                found = true;
            }
        } finally {
            if (!found) {
                poller.moveTo(originalEntryId);
            }
        }
        return found;
    }

    @Override
    public boolean moveToIndex(final long index) {
        if (index() == index) {
            return true;
        }
        final long originalEntryId = poller.entryId();
        boolean found = false;
        try {
            poller.moveToStart();
            long pollIndex = 0;
            while (pollIndex != index) {
                if (poller.poll(message -> Result.POLL) == 0) {
                    break;
                }
                pollIndex++;
            }
            if (pollIndex == index) {
                this.index = pollIndex;
                found = true;
            }
        } finally {
            if (!found) {
                poller.moveTo(originalEntryId);
            }
        }
        return found;
    }

    @Override
    public boolean moveToNext() {
        if (poller.moveToNext()) {
            index++;
            return true;
        }
        return false;
    }

    @Override
    public boolean moveToPrevious() {
        if (index > 0 && poller.moveToPrevious()) {
            index--;
            return true;
        }
        return false;
    }

    @Override
    public DefaultIndexTrackingPoller moveToStart() {
        poller.moveToStart();
        index = 0;
        return this;
    }

    @Override
    public Poller moveToEnd() {
        while (poller.moveToNext()) {
            index++;
        }
        return this;
    }

    @Override
    public int poll(final Handler handler) {
        final int polled = poller.poll(handler);
        if (polled > 0) {
            assert polled == 1;
            index++;
            return 1;
        }
        return 0;
    }

    @Override
    public void close() {
        poller.close();
    }
}
