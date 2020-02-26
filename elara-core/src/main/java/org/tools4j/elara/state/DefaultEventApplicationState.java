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
package org.tools4j.elara.state;

import org.agrona.collections.Long2LongHashMap;
import org.tools4j.elara.command.Command;

public class DefaultEventApplicationState implements EventApplicationState.Mutable {

    private final Long2LongHashMap inputToSeqMap = new Long2LongHashMap(NO_COMMANDS);

    @Override
    public long lastCommandAllEventsApplied(final int input) {
        return inputToSeqMap.get(input);
    }

    @Override
    public void allEventsAppliedFor(final Command.Id id) {
        final int input = id.input();
        final long sequence = id.sequence();
        if (sequence > lastCommandAllEventsApplied(input)) {
            inputToSeqMap.put(input, sequence);
        }
    }
}
