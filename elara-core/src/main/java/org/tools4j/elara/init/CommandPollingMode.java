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
package org.tools4j.elara.init;

/**
 * Defines how to resume command stream polling when restarting an application with an existing command store or stream.
 * <p>
 * The default mode is {@link #FROM_END} so that only new commands will be polled; commands existing in the command
 * store are skipped when starting the application.
 */
public enum CommandPollingMode {
    /** All commands in the command store are replayed */
    REPLAY_ALL,
    /** Resumes polling from the position of the last poll;  works only if this mode was used also previously */
    FROM_LAST,
    /** No replay at all, only newly added commands will be polled */
    FROM_END;

    /**
     * Poller ID used for {@link #FROM_LAST} mode.
     */
    public static final String DEFAULT_POLLER_ID = "elara-cmd";
}
