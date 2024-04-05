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
package org.tools4j.elara.plugin.timer;

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.plugin.timer.Timer.Style;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.util.Objects.requireNonNull;
import static org.agrona.collections.Hashing.DEFAULT_LOAD_FACTOR;

/**
 * Timer store backed by an {@link MutableDirectBuffer} to store timers. Each timer occupies a block of bytes for all
 * the timer attributes.  The store also maintains a mapping of timer ID to index from which one can derive the offset
 * in the buffer.
 */
public class DirectTimerStore implements TimerStore.MutableTimerStore {

    public static final int DEFAULT_CAPACITY = 64;
    private static final int START_TIME_OFFSET = 0;
    private static final int START_TIME_LENGTH = Long.BYTES;
    private static final int TIMER_PAYLOAD_OFFSET = START_TIME_OFFSET + START_TIME_LENGTH;
    private static final int TIMER_PAYLOAD_LENGTH = TimerPayloadDescriptor.PAYLOAD_SIZE;
    private static final int ENTRY_SIZE = TIMER_PAYLOAD_OFFSET + TIMER_PAYLOAD_LENGTH;

    private final Long2LongHashMap timerIdToTimerIndex;
    private final MutableDirectBuffer buffer;
    private int count;

    public DirectTimerStore() {
        this(DEFAULT_CAPACITY);
    }

    public DirectTimerStore(final int initialCapacity) {
        this(initialCapacity, new ExpandableDirectByteBuffer(initialCapacity * ENTRY_SIZE));
    }
    public DirectTimerStore(final int initialCapacity, final MutableDirectBuffer buffer) {
        this.timerIdToTimerIndex = new Long2LongHashMap(2 * initialCapacity, DEFAULT_LOAD_FACTOR, -1);
        this.buffer = requireNonNull(buffer);
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public boolean hasTimer(final long timerId) {
        return timerIdToTimerIndex.containsKey(timerId);
    }

    @Override
    public int index(final long timerId) {
        return (int)timerIdToTimerIndex.get(timerId);
    }

    private int offset(final int index) {
        return index * ENTRY_SIZE;
    }

    private int fieldOffset(final int index, final int fieldOffset) {
        return offset(index) + fieldOffset;
    }

    private int payloadOffset(final int index) {
        return fieldOffset(index, TIMER_PAYLOAD_OFFSET);
    }

    private long getLong(final int offset) {
        return buffer.getLong(offset, LITTLE_ENDIAN);
    }

    @Override
    public long timerId(final int index) {
        return FlyweightTimerPayload.timerId(buffer, payloadOffset(index));
    }

    @Override
    public Style style(final int index) {
        return FlyweightTimerPayload.style(buffer, payloadOffset(index));
    }

    @Override
    public int repetition(final int index) {
        return FlyweightTimerPayload.repetition(buffer, payloadOffset(index));
    }

    @Override
    public long startTime(final int index) {
        return getLong(fieldOffset(index, START_TIME_OFFSET));
    }

    @Override
    public long timeout(final int index) {
        return FlyweightTimerPayload.timeout(buffer, payloadOffset(index));
    }

    @Override
    public int timerType(final int index) {
        return FlyweightTimerPayload.timerType(buffer, payloadOffset(index));
    }

    @Override
    public long contextId(final int index) {
        return FlyweightTimerPayload.contextId(buffer, payloadOffset(index));
    }

    @Override
    public long deadline(final int index) {
        final int offset = offset(index);
        final int payloadOffset = offset + TIMER_PAYLOAD_OFFSET;
        final Style style = FlyweightTimerPayload.style(buffer, payloadOffset);
        final long timeout = FlyweightTimerPayload.timeout(buffer, payloadOffset);
        switch (style) {
            case ALARM:
                return timeout;
            case TIMER:
                return getLong(offset + START_TIME_OFFSET) + timeout;
            case PERIODIC:
                return getLong(offset + START_TIME_OFFSET) + timeout + timeout * FlyweightTimerPayload.repetition(buffer, payloadOffset);
            default:
                throw new IllegalArgumentException("Illegal style: " + style);
        }
    }

    @Override
    public boolean add(final long timerId, final Style style, final int repetition, final long startTime, final long timeout, final int timerType, final long contextId) {
        if (timerIdToTimerIndex.putIfAbsent(timerId, count) >= 0) {
            //timer with timerId is already present
            return false;
        }
        final int offset = offset(count);
        final int payloadOffset = offset + TIMER_PAYLOAD_OFFSET;
        buffer.putLong(offset + START_TIME_OFFSET, startTime, LITTLE_ENDIAN);
        switch (style) {
            case ALARM:
                FlyweightTimerPayload.writeAlarm(timerId, timeout, timerType, contextId, buffer, payloadOffset);
                break;
            case TIMER:
                FlyweightTimerPayload.writeTimer(timerId, timeout, timerType, contextId, buffer, payloadOffset);
                break;
            case PERIODIC:
                FlyweightTimerPayload.writePeriodic(timerId, timeout, repetition, timerType, contextId, buffer, payloadOffset);
                break;
        }
        count++;
        return true;
    }

    private void validateIndex(final int index) {
        if (index < 0 || index >= count) {
            throw new IndexOutOfBoundsException("Index " + index + " is not in [0.." + (count - 1) + "]");
        }
    }

    @Override
    public void remove(final int index) {
        validateIndex(index);
        final int curOffset = offset(index);
        final int lastOffset = offset(count - 1);
        final long curTimerId = FlyweightTimerPayload.timerId(buffer, curOffset + TIMER_PAYLOAD_OFFSET);

        //move last to current
        if (curOffset != lastOffset) {
            final long lastTimerId = FlyweightTimerPayload.timerId(buffer, lastOffset + TIMER_PAYLOAD_OFFSET);
            buffer.putBytes(curOffset, buffer, lastOffset, ENTRY_SIZE);
            timerIdToTimerIndex.put(lastTimerId, index);
        }
        //now remove last
        timerIdToTimerIndex.remove(curTimerId);
        count--;
    }

    @Override
    public void removeAll() {
        timerIdToTimerIndex.clear();
        count = 0;
    }

    @Override
    public void updateRepetition(final int index, final int repetition) {
        validateIndex(index);
        FlyweightTimerPayload.writeRepetition(repetition, buffer, payloadOffset(index));
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(256);
        builder.append("DirectTimerStore:timers=[");
        for (int i = 0; i < count; i++) {
            builder.append(i == 0 ? "" : ", ");
            builder.append(i);
            builder.append(":timer-id=").append(timerId(i));
            builder.append("|style=").append(style(i));
            builder.append("|repetition=").append(repetition(i));
            builder.append("|start-time=").append(startTime(i));
            builder.append("|timeout=").append(timeout(i));
            builder.append("|deadline=").append(deadline(i));
            builder.append("|timer-type=").append(timerType(i));
            builder.append("|context-id=").append(contextId(i));
        }
        builder.append("]");
        return builder.toString();
    }
}
