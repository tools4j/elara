/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2024 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.state;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Hashing;
import org.agrona.collections.Int2IntCounterMap;
import org.tools4j.elara.flyweight.CommandFrame;
import org.tools4j.elara.flyweight.EventType;

import java.nio.ByteOrder;

/**
 * In-flight state implementation backed by a {@link DirectBuffer} used as a ring buffer to append new sent entries on
 * one side and remove entries that are acknowledged through events on the other end.  The buffer capacity is expanded
 * dynamically if necessary.
 */
public class DefaultInFlightState implements MutableInFlightState {

    public static final int DEFAULT_INITIAL_SOURCE_ID_CAPACITY = 32;
    public static final int DEFAULT_INITIAL_IN_FLIGHT_CAPACITY = 256;
    private static final int INDEX_0 = 0;

    private static final int SOURCE_ID_OFFSET = 0;
    private static final int SOURCE_ID_LENGTH = Integer.BYTES;
    private static final int SOURCE_SEQ_OFFSET = SOURCE_ID_OFFSET + SOURCE_ID_LENGTH;
    private static final int SOURCE_SEQ_LENGTH = Long.BYTES;
    private static final int SENDING_TIME_OFFSET = SOURCE_SEQ_OFFSET + SOURCE_SEQ_LENGTH;
    private static final int SENDING_TIME_LENGTH = Long.BYTES;
    private static final int ENTRY_LENGTH = SENDING_TIME_OFFSET + SENDING_TIME_LENGTH;

    private final Int2IntCounterMap countBySourceId;
    private final MutableDirectBuffer buffer;
    private int offset;
    private int capacity;
    private int count;
    private long bytes;

    public DefaultInFlightState() {
        this(DEFAULT_INITIAL_SOURCE_ID_CAPACITY, DEFAULT_INITIAL_IN_FLIGHT_CAPACITY);
    }

    public DefaultInFlightState(final int sourceIdInitialCapacity, final int inFlightInitialCapacity) {
        this.countBySourceId = new Int2IntCounterMap(sourceIdInitialCapacity, Hashing.DEFAULT_LOAD_FACTOR, 0);
        this.buffer = new ExpandableDirectByteBuffer(inFlightInitialCapacity * ENTRY_LENGTH);
        this.capacity = inFlightInitialCapacity;
    }

    public void reset() {
        countBySourceId.clear();
        if (count > 0) {
            final int overlap = Math.max(0, offset + count - capacity);
            buffer.setMemory(offset * ENTRY_LENGTH, (count - overlap) * ENTRY_LENGTH, (byte)0);
            buffer.setMemory(0, overlap * ENTRY_LENGTH, (byte)0);
        }
        offset = 0;
        count = 0;
        bytes = 0L;
    }

    @Override
    public int inFlightCommands() {
        return count;
    }

    @Override
    public int inFlightCommands(final int sourceId) {
        return countBySourceId.get(sourceId);
    }

    @Override
    public long inFlightBytes() {
        return bytes;
    }

    private int byteOffset(final int index) {
        assert index >= 0;
        int realIndex = offset + index;
        if (realIndex >= capacity) {
            realIndex -= capacity;
        }
        assert realIndex >= 0 && realIndex < capacity;
        return realIndex * ENTRY_LENGTH;
    }

    private void validateIndex(final int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in [0, " + (count - 1) + "]");
        }
    }

    @Override
    public int sourceId(final int index) {
        validateIndex(index);
        return buffer.getInt(byteOffset(index) + SOURCE_ID_OFFSET, ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public long sourceSequence(final int index) {
        validateIndex(index);
        return buffer.getLong(byteOffset(index) + SOURCE_SEQ_OFFSET, ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public long sendingTime(final int index) {
        validateIndex(index);
        return buffer.getLong(byteOffset(index) + SENDING_TIME_OFFSET, ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public void onCommandSent(final int sourceId, final long sourceSequence, final long sendingTime, final int payloadSize) {
        if (count == capacity) {
            expandBuffer(1);
        }
        assert count < capacity;
        final int byteOffset = byteOffset(count);
        buffer.putInt(byteOffset + SOURCE_ID_OFFSET, sourceId, ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(byteOffset + SOURCE_SEQ_OFFSET, sourceSequence, ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(byteOffset + SENDING_TIME_OFFSET, sendingTime, ByteOrder.LITTLE_ENDIAN);
        count++;
        bytes += (CommandFrame.HEADER_LENGTH + payloadSize);
        final int newCount = countBySourceId.incrementAndGet(sourceId);
        assert newCount > 0;
    }

    private void expandBuffer(final int n) {
        final int overlap = Math.max(0, offset + count - capacity);
        final int expand = Math.max(n, overlap);//need some space to copy overlap entries to the end
        buffer.checkLimit((count + expand) * ENTRY_LENGTH);
        if (overlap > 0) {
            //move overlap entries to the end of the new expanded buffer
            final int overlapLength = overlap * ENTRY_LENGTH;
            buffer.putBytes(capacity * ENTRY_LENGTH, buffer, 0, overlapLength);
            buffer.setMemory(0, overlapLength, (byte) 0);
        }
        capacity = buffer.capacity() / ENTRY_LENGTH;
        assert offset + count <= capacity;
    }

    @Override
    public void onEvent(final int srcId, final long srcSeq, final long evtSeq, final int index, final EventType evtType, final long evtTime, final int payloadType, final int payloadSize) {
        if (count == 0 || !evtType.isLast()) {
            return;
        }
        final boolean invalidSequence;
        final int firstSourceId = sourceId(INDEX_0);
        if (srcId == firstSourceId) {
            final long firstSourceSeq = sourceSequence(INDEX_0);
            if (firstSourceSeq == srcSeq) {
                removeFirst(srcId, payloadSize);
                return;
            }
            invalidSequence = true;
        } else {
            invalidSequence = countBySourceId.get(srcId) > 0;
        }
        if (invalidSequence) {
            //we have received an event out-of sequence
            throw new IllegalStateException("Received " +
                    "event:evt-seq=" + evtSeq + "|src-id=" + srcId + "|src-seq=" + srcSeq +
                    " but expected an event first for a prior in-flight " +
                    "command:src-id=" + firstSourceId + "|src-seq=" + sourceSequence(INDEX_0));
        }
    }

    private void removeFirst(final int srcId, final int payloadSize) {
        final int newCount = countBySourceId.decrementAndGet(srcId);
        assert newCount >= 0;
        buffer.setMemory(byteOffset(INDEX_0), ENTRY_LENGTH, (byte)0);
        offset++;
        count--;
        bytes-=(CommandFrame.HEADER_LENGTH + payloadSize);
        if (count == 0) {
            offset = 0;
            bytes = 0;
        }
    }

    @Override
    public String toString() {
        return "DefaultInFlightState" +
                ":count=" + count +
                "|bytes=" + bytes +
                "|count-by-source-id=" + countBySourceId;
    }
}
