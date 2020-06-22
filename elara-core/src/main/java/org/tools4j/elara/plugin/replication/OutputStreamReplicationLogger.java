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
package org.tools4j.elara.plugin.replication;

import java.io.PrintStream;

import static java.util.Objects.requireNonNull;

public class OutputStreamReplicationLogger implements ReplicationLogger, AutoCloseable {

    private final PrintStream warn;
    private final boolean closeOnLoggerClose;
    private final PlaceholderReplacer replacer = new PlaceholderReplacer(128);

    public OutputStreamReplicationLogger() {
        this(System.err, false);
    }

    public OutputStreamReplicationLogger(final PrintStream warn, final boolean closeOnLoggerClose) {
        this.warn = requireNonNull(warn);
        this.closeOnLoggerClose = closeOnLoggerClose;
    }

    @Override
    public void warn(final String message, final long arg) {
        warn.println(replacer.init(message).replace(arg).exit());
    }

    @Override
    public void warn(final String message, final long arg0, final long arg1) {
        warn.println(replacer.init(message).replace(arg0).replace(arg1).exit());
    }

    @Override
    public void warn(final String message, final long arg0, final long arg1, final long arg2) {
        warn.println(replacer.init(message).replace(arg0).replace(arg1).replace(arg2).exit());
    }

    @Override
    public void close() throws Exception {
        if (closeOnLoggerClose) {
            warn.close();
        }
    }

    private static final class PlaceholderReplacer {
        private final StringBuilder temp;
        private String message;
        private int start;
        PlaceholderReplacer(final int initialCapacity) {
            this.temp = new StringBuilder(initialCapacity);
        }
        PlaceholderReplacer init(final String message) {
            this.message = message;
            this.start = 0;
            return this;
        }
        PlaceholderReplacer replace(final long arg) {
            final int index = message.indexOf(PLACEHOLDER, start);
            if (index >= 0) {
                temp.append(message, start, index);
                temp.append(arg);
                start = index + PLACEHOLDER.length();
            } else {
                temp.append(message, start, message.length());
                start = message.length();
            }
            return this;
        }
        StringBuilder exit() {
            temp.append(message, start, message.length());
            return temp;
        }
    }
}
