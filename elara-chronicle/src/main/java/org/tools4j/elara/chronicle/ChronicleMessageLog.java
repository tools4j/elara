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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import org.tools4j.elara.log.MessageLog;

import static java.util.Objects.requireNonNull;

public class ChronicleMessageLog implements MessageLog {

    private final ChronicleQueue queue;
    private final ThreadLocal<ChronicleLogAppender> appender;

    public ChronicleMessageLog(final ChronicleQueue queue) {
        this.queue = requireNonNull(queue);
        this.appender = ThreadLocal.withInitial(() -> new ChronicleLogAppender(queue));
    }

    public ChronicleQueue queue() {
        return queue;
    }

    @Override
    public ChronicleLogAppender appender() {
        return appender.get();
    }

    @Override
    public ChronicleLogPoller poller() {
        return new ChronicleLogPoller(queue);
    }

    @Override
    public Poller poller(final String id) {
        return new ChronicleLogPoller(id, queue);
    }

    @Override
    public void close() {
        queue.close();
    }

    @Override
    public String toString() {
        return "ChronicleMessageLog{queue=" + queue + '}';
    }
}
