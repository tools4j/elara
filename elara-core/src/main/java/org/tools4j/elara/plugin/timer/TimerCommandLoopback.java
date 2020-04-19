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

import org.agrona.ExpandableDirectByteBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.output.Output;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerCommandDescriptor.TIMER_PAYLOAD_SIZE;
import static org.tools4j.elara.plugin.timer.TimerCommands.triggerTimer;

public class TimerCommandLoopback implements Output {

    private final TimerState.Mutable timerState;
    private final MutableDirectBuffer buffer = new ExpandableDirectByteBuffer(TIMER_PAYLOAD_SIZE);

    public TimerCommandLoopback(final TimerState.Mutable timerState) {
        this.timerState = requireNonNull(timerState);
    }

    @Override
    public void publish(final Event event, final CommandLoopback loopback) {
        final int next = timerState.indexOfNextDeadline();
        if (next >= 0 && timerState.deadline(next) <= event.time()) {
            final int len = triggerTimer(buffer, 0, timerState.id(next), timerState.type(next), timerState.timeout(next));
            loopback.enqueueCommand(TimerCommands.TRIGGER_TIMER, buffer, 0, len);
        }
    }
}
