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
package org.tools4j.elara.chronicle;

import net.openhft.chronicle.queue.ChronicleQueue;
import net.openhft.chronicle.queue.ExcerptTailer;
import org.tools4j.elara.log.Flyweight;
import org.tools4j.elara.log.PeekableMessageLog;
import org.tools4j.elara.log.Writable;

import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class ChronicleMessageLog<M extends Writable> implements PeekableMessageLog<M> {

    private final ChronicleQueue queue;
    private final Supplier<? extends Flyweight<? extends M>> flyweightSupplier;
    private ExcerptTailer sizeTailer; //lazy init

    public ChronicleMessageLog(final ChronicleQueue queue,
                               final Supplier<? extends Flyweight<? extends M>> flyweightSupplier) {
        this.queue = requireNonNull(queue);
        this.flyweightSupplier = requireNonNull(flyweightSupplier);
    }

    @Override
    public Appender<M> appender() {
        return new ChronicleLogAppender<M>(queue);
    }

    @Override
    public PeekableMessageLog.PeekablePoller<M> poller() {
        return new ChronicleLogPoller<M>(queue, flyweightSupplier.get());
    }

    @Override
    public PeekablePoller<M> poller(final String id) {
        return new ChronicleLogPoller<M>(id, queue, flyweightSupplier.get());
    }

    @Override
    public long size() {
        if (sizeTailer == null) {
            if (queue.firstIndex() == Long.MAX_VALUE) {
                return 0;
            }
            sizeTailer = queue.createTailer();
        }
        sizeTailer.toEnd();
        return sizeTailer.index() + 1;
    }

    @Override
    public void close() {
        queue.close();
    }
}
