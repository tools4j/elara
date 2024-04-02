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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.flyweight.Flyweight;
import org.tools4j.elara.flyweight.Writable;
import org.tools4j.elara.logging.Printable;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static org.tools4j.elara.flyweight.TimeMetricsDescriptor.HEADER_LENGTH;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.CONTEXT_ID_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.FLAG_ALARM;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.FLAG_NONE;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.FLAG_PERIODIC;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.PAYLOAD_SIZE;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.REPETITION_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMEOUT_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_ID_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TYPE_OFFSET;

/**
 * A flyweight for reading and writing timer payload data laid out as per {@link TimerPayloadDescriptor}.
 */
public class FlyweightTimerPayload implements Timer, Flyweight<FlyweightTimerPayload>, Printable, Writable {

    private final MutableDirectBuffer buffer = new UnsafeBuffer(0, 0);

    @Override
    public FlyweightTimerPayload wrap(final DirectBuffer buffer, final int offset) {
        this.buffer.wrap(buffer, offset, PAYLOAD_SIZE);
        return this;
    }

    @Override
    public boolean valid() {
        return buffer.capacity() >= PAYLOAD_SIZE;
    }

    @Override
    public FlyweightTimerPayload reset() {
        buffer.wrap(0, 0);
        return this;
    }

    @Override
    public long timerId() {
        return timerId(buffer, 0);
    }

    public static long timerId(final DirectBuffer buffer, final int offset) {
        return buffer.getLong(offset + TIMER_ID_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public Style style() {
        return style(buffer, 0);
    }

    public static Style style(final DirectBuffer buffer, final int offset) {
        return style(buffer.getInt(offset + REPETITION_OFFSET, LITTLE_ENDIAN));

    }
    public static Style style(final int repetitionRaw) {
        return (FLAG_PERIODIC & repetitionRaw) != 0 ? Style.PERIODIC :
                (FLAG_ALARM & repetitionRaw) != 0 ? Style.ALARM : Style.TIMER;
    }

    @Override
    public long timeout() {
        return timeout(buffer, 0);
    }

    public static long timeout(final DirectBuffer buffer, final int offset) {
        return buffer.getLong(offset + TIMEOUT_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int timerType() {
        return timerType(buffer, 0);
    }

    public static int timerType(final DirectBuffer buffer, final int offset) {
        return buffer.getInt(offset + TIMER_TYPE_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int repetition() {
        return repetition(buffer, 0);
    }

    public static int repetition(final DirectBuffer buffer, final int offset) {
        return repetition(buffer.getInt(offset + REPETITION_OFFSET, LITTLE_ENDIAN));
    }

    public static int repetition(final int repetitionRaw) {
        return (~FLAG_PERIODIC) & repetitionRaw;
    }

    @Override
    public long contextId() {
        return contextId(buffer, 0);
    }

    public static long contextId(final DirectBuffer buffer, final int offset) {
        return buffer.getLong(offset + CONTEXT_ID_OFFSET, LITTLE_ENDIAN);
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        dst.putBytes(dstOffset, buffer, 0, PAYLOAD_SIZE);
        return PAYLOAD_SIZE;
    }

    public static int writeAlarm(final long timerId,
                                 final long time,
                                 final int timerType,
                                 final long contextId,
                                 final MutableDirectBuffer dst,
                                 final int dstOffset) {
        return write(timerId, time, FLAG_ALARM, timerType, contextId, dst, dstOffset);
    }

    public static int writeTimer(final long timerId,
                                 final long time,
                                 final int timerType,
                                 final long contextId,
                                 final MutableDirectBuffer dst,
                                 final int dstOffset) {
        return write(timerId, time, FLAG_NONE, timerType, contextId, dst, dstOffset);
    }
    public static int writePeriodic(final long timerId,
                                    final long time,
                                    final int repetition,
                                    final int timerType,
                                    final long contextId,
                                    final MutableDirectBuffer dst,
                                    final int dstOffset) {
        return write(timerId, time, FLAG_PERIODIC | repetition, timerType, contextId, dst, dstOffset);
    }

    private static int write(final long timerId,
                             final long time,
                             final int repetition,
                             final int timerType,
                             final long contextId,
                             final MutableDirectBuffer dst,
                             final int dstOffset) {
        dst.putLong(dstOffset + TIMER_ID_OFFSET, timerId, LITTLE_ENDIAN);
        dst.putLong(dstOffset + TIMEOUT_OFFSET, time, LITTLE_ENDIAN);
        dst.putInt(dstOffset + TIMER_TYPE_OFFSET, timerType, LITTLE_ENDIAN);
        dst.putInt(dstOffset + REPETITION_OFFSET, repetition, LITTLE_ENDIAN);
        dst.putLong(dstOffset + CONTEXT_ID_OFFSET, contextId, LITTLE_ENDIAN);
        return HEADER_LENGTH;
    }

    public static void writeRepetition(final int repetition, final MutableDirectBuffer dst, final int payloadOffset) {
        dst.putInt(payloadOffset + REPETITION_OFFSET, FLAG_PERIODIC | repetition, LITTLE_ENDIAN);
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightTimerPayload");
        if (valid()) {
            final Style style = style();
            dst.append(":timer-id=").append(timerId());
            dst.append("|style=").append(style);
            if (style == Style.ALARM) {
                dst.append("|time=").append(timeout());
            } else {
                dst.append("|timeout=").append(timeout());
            }
            if (style == Style.PERIODIC) {
                dst.append("|repetition=").append(repetition());
            }
            dst.append("|timer-type=").append(timerType());
            dst.append("|context-id=").append(contextId());
        } else {
            dst.append(":???");
        }
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }
}
