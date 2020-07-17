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
import org.tools4j.elara.flyweight.Frame;

import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.PAYLOAD_SIZE;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_ID_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_REPETITION_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TIMEOUT_OFFSET;
import static org.tools4j.elara.plugin.timer.TimerPayloadDescriptor.TIMER_TYPE_OFFSET;

/**
 * Timer commands issued through {@link TimerTriggerInput} either when outputting an event through the
 * command loopback or when polling the timer trigger input.
 */
public enum TimerCommands {
    ;
    /** Command issued by {@link TimerTriggerInput}; its processing subsequently triggers a {@link TimerEvents#TIMER_EXPIRED} event.*/
    public static final int TRIGGER_TIMER = -10;

    public static int triggerTimer(final MutableDirectBuffer buffer,
                                   final int offset,
                                   final long timerId,
                                   final int timerType,
                                   final int repetition,
                                   final long timeout) {
        buffer.putLong(offset + TIMER_ID_OFFSET, timerId);
        buffer.putInt(offset + TIMER_TYPE_OFFSET, timerType);
        buffer.putInt(offset + TIMER_REPETITION_OFFSET, repetition);
        buffer.putLong(offset + TIMER_TIMEOUT_OFFSET, timeout);
        return PAYLOAD_SIZE;
    }

    public static long timerId(final Command command) {
        return TimerPayloadDescriptor.timerId(command.payload());
    }

    public static int timerType(final Command command) {
        return TimerPayloadDescriptor.timerType(command.payload());
    }

    public static int timerRepetition(final Command command) {
        return TimerPayloadDescriptor.timerRepetition(command.payload());
    }

    public static long timerTimeout(final Command command) {
        return TimerPayloadDescriptor.timerTimeout(command.payload());
    }

    public static boolean isTimerCommand(final Command command) {
        return isTimerCommandType(command.type());
    }

    public static boolean isTimerCommand(final Frame frame) {
        return frame.header().index() < 0 && isTimerCommandType(frame.header().type());
    }

    public static boolean isTimerCommandType(final int commandType) {
        switch (commandType) {
            case TRIGGER_TIMER:
                return true;
            default:
                return false;
        }
    }

    public static String timerCommandName(final Command command) {
        return timerCommandName(command.type());
    }

    public static String timerCommandName(final Frame frame) {
        return timerCommandName(frame.header().type());
    }

    public static String timerCommandName(final int commandType) {
        switch (commandType) {
            case TRIGGER_TIMER:
                return "TRIGGER_TIMER";
            default:
                throw new IllegalArgumentException("Not a timer command type: " + commandType);
        }
    }
}
