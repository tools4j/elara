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
import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.SequenceGenerator;
import org.tools4j.elara.time.TimeSource;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.timer.TimerCommandDescriptor.TIMER_PAYLOAD_SIZE;
import static org.tools4j.elara.plugin.timer.TimerCommands.triggerTimer;

public final class TimerTriggerInput implements Input {

    private final TimeSource timeSource;
    private final TimerState timerState;
    private final SequenceGenerator adminSequenceGenerator;

    private final MutableDirectBuffer buffer = new ExpandableDirectByteBuffer(TIMER_PAYLOAD_SIZE);
    private final Poller poller = new TimerTriggerPoller();

    public TimerTriggerInput(final TimeSource timeSource,
                             final TimerState timerState,
                             final SequenceGenerator adminSequenceGenerator) {
        this.timeSource = requireNonNull(timeSource);
        this.timerState = requireNonNull(timerState);
        this.adminSequenceGenerator = requireNonNull(adminSequenceGenerator);
    }

    @Override
    public int id() {
        return ADMIN_ID;
    }

    @Override
    public Poller poller() {
        return poller;
    }

    private class TimerTriggerPoller implements Poller {
        int roundRobin = 0;
        @Override
        public int poll(final Handler handler) {
            final int count = timerState.count();
            if (count == 0) {
                return 0;
            }
            if (roundRobin >= count) {
                roundRobin = 0;
            }
            final long time = timeSource.currentTime();
            for (int j = 0; j < count; j++) {
                final int i = roundRobin;
                final long deadline = timerState.time(i) + timerState.timeout(i);
                if (deadline <= time) {
                    final long sequence = adminSequenceGenerator.nextSequence();
                    final int len = triggerTimer(buffer, 0, timerState.type(i), timerState.id(i), timerState.timeout(i));
                    handler.onMessage(sequence, TimerCommands.TRIGGER_TIMER, buffer, 0, len);
                    return 1;
                }
                roundRobin++;
                if (roundRobin >= count) {
                    roundRobin = 0;
                }
            }
            return 0;
        }
    }
}
