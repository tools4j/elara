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
package org.tools4j.elara.log;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.log.PeekableMessageLog.PeekPollHandler.Result;

import java.util.ArrayDeque;
import java.util.Queue;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.log.PeekableMessageLog.PeekPollHandler.Result.POLL;

public class InMemoryLog<M extends Writable> implements PeekableMessageLog<M> {

    private final Flyweight<? extends M> flyweight;
    private final Queue<DirectBuffer> queue = new ArrayDeque<>();

    public InMemoryLog(final Flyweight<? extends M> flyweight) {
        this.flyweight = requireNonNull(flyweight);
    }

    @Override
    public Appender<M> appender() {
        return message -> {
            final MutableDirectBuffer buffer = new ExpandableArrayBuffer();
            message.writeTo(buffer, 0);
            queue.add(buffer);
        };
    }

    @Override
    public PeekablePoller<M> poller() {
        return new PeekablePoller<M>() {
            @Override
            public int peekOrPoll(final PeekPollHandler<? super M> handler) {
                final DirectBuffer message = queue.peek();
                if (message != null) {
                    final M flyMessage = flyweight.init(message, 0);
                    final Result result = handler.onMessage(flyMessage);
                    if (result == POLL) {
                        queue.poll();
                        return 1;
                    }
                    //NOTE: we have work done here, but if this work is the only
                    //      bit performed in the duty cycle loop then the result
                    //      in the next loop iteration will be the same, hence we
                    //      better let the idle strategy do its job
                }
                return 0;
            }

            @Override
            public int poll(final Handler<? super M> handler) {
                final DirectBuffer message = queue.poll();
                if (message != null) {
                    final M flyMessage = flyweight.init(message, 0);
                    handler.onMessage(flyMessage);
                    return 1;
                }
                return 0;
            }
        };
    }

    @Override
    public long size() {
        return queue.size();
    }
}
