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
package org.tools4j.elara.log;

import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.command.Command;

import static java.util.Objects.requireNonNull;

public class InputTrackingAppender implements MessageLog.Appender<Command>, MessageLog.Handler<Command> {

    private final MessageLog.Appender<? super Command> appender;
    private final Long2LongHashMap inputToSeqMap = new Long2LongHashMap(Long.MIN_VALUE);

    public InputTrackingAppender(final MessageLog.Appender<? super Command> appender,
                                 final MessageLog.Poller<? extends Command> poller) {
        this.appender = requireNonNull(appender);
        while (poller.poll(this) > 0) {
            //keep going until we have updated the whole map
        }
    }

    @Override
    public void append(final Command command) {
        if (update(command)) {
            appender.append(command);
        }
    }

    @Override
    public void onMessage(final Command command) {
        update(command);
    }

    private boolean update(final Command command) {
        final int input = command.id().input();
        final long currentSeq = command.id().sequence();
        final long lastAppendedSeq = inputToSeqMap.get(input);
        if (lastAppendedSeq < currentSeq) {
            inputToSeqMap.put(input, currentSeq);
            return true;
        }
        return false;
    }
}
