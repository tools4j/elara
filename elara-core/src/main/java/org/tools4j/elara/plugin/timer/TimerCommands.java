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

import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.flyweight.DataFrame;

import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;

/**
 * Timer commands sent by {@link TimerSignalPoller} and {@link TimerController}.
 */
public enum TimerCommands {
    ;
    /** Payload type for command to start a timer.*/
    public static final int START_TIMER = TimerPayloadTypes.START_TIMER;
    /** Payload type for command to cancel a timer.*/
    public static final int CANCEL_TIMER = TimerPayloadTypes.CANCEL_TIMER;
    /** Payload type for command to signal that a timer should trigger or be expired.*/
    public static final int SIGNAL_TIMER = TimerPayloadTypes.SIGNAL_TIMER;

    public static boolean isTimerCommand(final Command command) {
        return isTimerCommand(command.payloadType());
    }

    public static boolean isTimerCommand(final DataFrame frame) {
        return frame.type() == COMMAND_TYPE && isTimerCommand(frame.payloadType());
    }

    public static boolean isTimerCommand(final int payloadType) {
        return TimerPayloadTypes.isTimerPayloadType(payloadType);
    }

    public static String timerCommandName(final Command command) {
        return timerCommandName(command.payloadType());
    }

    public static String timerCommandName(final DataFrame frame) {
        return timerCommandName(frame.payloadType());
    }

    public static String timerCommandName(final int payloadType) {
        switch (payloadType) {
            case START_TIMER:
                return "START_TIMER";
            case CANCEL_TIMER:
                return "CANCEL_TIMER";
            case SIGNAL_TIMER:
                return "SIGNAL_TIMER";
            default:
                throw new IllegalArgumentException("Not a timer command type: " + payloadType);
        }
    }
}
