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
package org.tools4j.elara.store;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.stream.MessageStream.Handler.Result;

import static org.tools4j.elara.stream.MessageStream.Handler.Result.POLL;

public class InMemoryStore implements MessageStore {

    public static final int DEFAULT_INITIAL_QUEUE_CAPACITY = 16;
    public static final int DEFAULT_INITIAL_BUFFER_CAPACITY = 256;

    private final int initialBufferCapacity;
    private final boolean removeOnPoll;
    private final boolean initEagerly;
    private int[] lengths;
    private MutableDirectBuffer[] buffers;
    private int start;
    private int size;

    public InMemoryStore() {
        this(DEFAULT_INITIAL_QUEUE_CAPACITY, DEFAULT_INITIAL_BUFFER_CAPACITY, false, false);
    }

    public InMemoryStore(final int initialQueueCapacity,
                         final int initialBufferCapacity,
                         final boolean removeOnPoll,
                         final boolean initEagerly) {
        this.initialBufferCapacity = initialBufferCapacity;
        this.removeOnPoll = removeOnPoll;
        this.initEagerly = initEagerly;
        this.lengths = new int[initialQueueCapacity];
        this.buffers = new MutableDirectBuffer[initialQueueCapacity];
        if (initEagerly) {
            for (int i = 0; i < initialQueueCapacity; i++) {
                buffers[i] = new ExpandableArrayBuffer(initialBufferCapacity);
            }
        }
    }

    @Override
    public Appender appender() {
        ensureMessageStoreNotClosed();
        return new Appender() {
            boolean closed;
            final AppendingContext appendContext = new AppendingContext();
            @Override
            public void append(final DirectBuffer buffer, final int offset, final int length) {
                ensureNotClosed();
                final int ix;
                if (size < buffers.length) {
                    int index = start + size;
                    if (index >= buffers.length) {
                        index -= buffers.length;
                    }
                    ix = index;
                } else {
                    assert size == buffers.length;
                    final int newLen = extendedQueueCapacity(buffers.length);
                    final int[] newLengths = new int[newLen];
                    final MutableDirectBuffer[] newBuffers = new MutableDirectBuffer[newLen];
                    System.arraycopy(lengths, start, newLengths, 0, size - start);
                    System.arraycopy(lengths, 0, newLengths, size - start, start);
                    System.arraycopy(buffers, start, newBuffers, 0, size - start);
                    System.arraycopy(buffers, 0, newBuffers, size - start, start);
                    if (initEagerly) {
                        for (int i = size; i < newLen; i++) {
                            newBuffers[i] = new ExpandableArrayBuffer(initialBufferCapacity);
                        }
                    }
                    start = 0;
                    lengths = newLengths;
                    buffers = newBuffers;
                    ix = size;
                }
                notNull(ix, length).putBytes(0, buffer, offset, length);
                lengths[ix] = length;
                size++;
            }

            @Override
            public AppendingContext appending() {
                ensureNotClosed();
                return appendContext.init();
            }

            final class AppendingContext implements MessageStore.AppendingContext {

                MutableDirectBuffer buffer;
                int index = -1;

                void reset() {
                    buffer = null;
                    index = -1;
                }

                AppendingContext init() {
                    if (buffer != null) {
                        abort();
                        throw new IllegalStateException("Aborted unclosed append context");
                    }
                    final int ix;
                    if (size < buffers.length) {
                        int index = start + size;
                        if (index >= buffers.length) {
                            index -= buffers.length;
                        }
                        ix = index;
                    } else {
                        assert size == buffers.length;
                        final int newLen = extendedQueueCapacity(buffers.length);
                        final int[] newLengths = new int[newLen];
                        final MutableDirectBuffer[] newBuffers = new MutableDirectBuffer[newLen];
                        System.arraycopy(lengths, start, newLengths, 0, size - start);
                        System.arraycopy(lengths, 0, newLengths, size - start, start);
                        System.arraycopy(buffers, start, newBuffers, 0, size - start);
                        System.arraycopy(buffers, 0, newBuffers, size - start, start);
                        if (initEagerly) {
                            for (int i = size; i < newLen; i++) {
                                newBuffers[i] = new ExpandableArrayBuffer(initialBufferCapacity);
                            }
                        }
                        start = 0;
                        lengths = newLengths;
                        buffers = newBuffers;
                        ix = size;
                    }
                    buffer = notNull(ix, 0);
                    lengths[ix] = 0;
                    index = ix;
                    return this;
                }

                @Override
                public MutableDirectBuffer buffer() {
                    if (buffer != null) {
                        return buffer;
                    }
                    throw new IllegalStateException("Append context is closed");
                }

                @Override
                public void commit(final int length) {
                    if (index < 0) {
                        throw new IllegalStateException("Append context is closed");
                    }
                    ensureNotClosed();
                    lengths[index] = length;
                    size++;
                    reset();
                }

                @Override
                public void abort() {
                    if (index < 0) {
                        throw new IllegalStateException("Append context is closed");
                    }
                    reset();
                }

                @Override
                public boolean isClosed() {
                    return buffer == null;
                }
            }

            void ensureNotClosed() {
                if (closed) {
                    throw new IllegalStateException("Appender is closed");
                }
                ensureMessageStoreNotClosed();
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }

    @Override
    public MessageStore.Poller poller() {
        ensureMessageStoreNotClosed();
        return new Poller() {
            boolean closed;
            final MutableDirectBuffer message = new UnsafeBuffer(0, 0);
            int index = 0;

            @Override
            public long entryId() {
                return index;
            }

            @Override
            public Poller moveToStart() {
                index = 0;
                return this;
            }

            @Override
            public Poller moveToEnd() {
                index = size;
                return this;
            }

            @Override
            public boolean moveToNext() {
                int next = index + 1;
                if (next >= size) {
                    return false;
                }
                index = next;
                return true;
            }

            @Override
            public boolean moveToPrevious() {
                int next = index - 1;
                if (next < 0) {
                    return false;
                }
                index = next;
                return true;
            }

            @Override
            public boolean moveTo(final long entryId) {
                if (entryId < 0) {
                    return false;
                }
                if (entryId < size) {
                    index = (int)entryId;
                    return true;
                }
                return false;
            }

            private void doMoveToNext() {
                if (removeOnPoll) {
                    start = start < buffers.length ? start + 1 : 0;
                    size--;
                } else {
                    index++;
                }
            }

            private int position() {
                final int pos = start + index;
                return pos < buffers.length ? pos : pos - buffers.length;
            }

            @Override
            public int poll(final Handler handler) {
                ensureNotClosed();
                if (index < size) {
                    final int pos = position();
                    final int length = lengths[pos];
                    final DirectBuffer buffer = buffers[pos];
                    message.wrap(buffer, 0, length);
                    final Result result = handler.onMessage(message);
                    message.wrap(0, 0);
                    if (result == POLL) {
                        doMoveToNext();
                        return 1;
                    }
                    //NOTE: we have work done here, but if this work is the only
                    //      bit performed in the duty cycle loop then the result
                    //      in the next loop iteration will be the same, hence we
                    //      better let the idle strategy do its job
                }
                return 0;
            }

            void ensureNotClosed() {
                if (closed) {
                    throw new IllegalStateException("Poller is closed");
                }
                ensureMessageStoreNotClosed();
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }

    @Override
    public Poller poller(final String id) {
        throw new UnsupportedOperationException("tracking poller not supported");
    }

    public long size() {
        return size;
    }

    @Override
    public void close() {
        size = 0;
        start = 0;
        buffers = null;
    }

    private void ensureMessageStoreNotClosed() {
        if (buffers == null) {
            throw new IllegalStateException("InMemoryStore is closed");
        }
    }

    private MutableDirectBuffer notNull(final int index, final int minCapacity) {
        final MutableDirectBuffer buffer = buffers[index];
        if (buffer != null) {
            return buffer;
        }
        return buffers[index] = new ExpandableArrayBuffer(Math.max(initialBufferCapacity, minCapacity));
    }

    private static int extendedQueueCapacity(final int length) {
        return (int)(Math.min(length * 2L, Integer.MAX_VALUE));
    }
}
