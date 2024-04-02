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

/**
 * Descriptor of payload data for timer commands and events in a byte buffer.
 * <p>
 * <br>
 * <pre>

 0         1         2         3         4         5         6
 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4 6 8 0 2 4
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 |                           Timer ID                            |
 +-------+-------+-------+-------+-------+-------+-------+-------+
 |                            Timeout                            |
 +-------+-------+-------+-------+-------+-------+-------+-------+
 |          Timer Type           |PA         Repetition          |
 +-------+-------+-------+-------+-------+-------+-------+-------+
 |                          Context ID                           |
 +-------+-------+-------+-------+-------+-------+-------+-------+

 * </pre>
 */
public enum TimerPayloadDescriptor {
    ;

    public static final int FLAG_PERIODIC = 0x80000000;
    public static final int FLAG_ALARM = 0x40000000;
    public static final int FLAG_NONE = 0x00000000;

    public static final int TIMER_ID_OFFSET = 0;
    public static final int TIMER_ID_LENGTH = Long.BYTES;
    public static final int TIMEOUT_OFFSET = TIMER_ID_OFFSET + TIMER_ID_LENGTH;
    public static final int TIMEOUT_LENGTH = Long.BYTES;
    public static final int TIMER_TYPE_OFFSET = TIMEOUT_OFFSET + TIMEOUT_LENGTH;
    public static final int TIMER_TYPE_LENGTH = Integer.BYTES;
    public static final int REPETITION_OFFSET = TIMER_TYPE_OFFSET + TIMER_TYPE_LENGTH;
    public static final int REPETITION_LENGTH = Integer.BYTES;
    public static final int CONTEXT_ID_OFFSET = REPETITION_OFFSET + REPETITION_LENGTH;
    public static final int CONTEXT_ID_LENGTH = Long.BYTES;

    public static final int PAYLOAD_SIZE = CONTEXT_ID_OFFSET + CONTEXT_ID_LENGTH;
}
