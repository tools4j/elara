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
package org.tools4j.elara.plugin.timer;

import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;

import static org.tools4j.elara.plugin.timer.TimerCommandDescriptor.*;

/**
 * Timer commands issued through {@link TimerTriggerInput}.
 */
public enum TimerCommands {
    ;
    /** Command issued by {@link TimerTriggerInput}; its processing subsequently triggers a {@link TimerEvents#TIMER_EXPIRED} event.*/
    public static final int TRIGGER_TIMER = -10;

    public static int triggerTimer(final MutableDirectBuffer buffer,
                                   final int offset,
                                   final int timerType,
                                   final long timerId,
                                   final long timeout) {
        buffer.putInt(offset + TIMER_TYPE_OFFSET, timerType);
        buffer.putLong(offset + TIMER_ID_OFFSET, timerId);
        buffer.putLong(offset + TIMER_TIMEOUT_OFFSET, timeout);
        return TIMER_PAYLOAD_SIZE;
    }

    public static int timerType(final Command command) {
        return command.payload().getInt(TIMER_TYPE_OFFSET);
    }
    public static long timerId(final Command command) {
        return command.payload().getLong(TIMER_ID_OFFSET);
    }
    public static long timerTimeout(final Command command) {
        return command.payload().getLong(TIMER_TIMEOUT_OFFSET);
    }
}
