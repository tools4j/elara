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
package org.tools4j.elara.loop;

import org.tools4j.elara.input.Input;
import org.tools4j.elara.input.Receiver;
import org.tools4j.nobark.loop.Step;

import static java.util.Objects.requireNonNull;

public final class SequencerStep implements Step {

    private final Receiver receiver;
    private final Input.Poller[] inputPollers;

    private int roundRobinIndex = 0;

    public SequencerStep(final Receiver receiver, final Input... inputs) {
        this.receiver = requireNonNull(receiver);
        this.inputPollers = initPollersFor(inputs);
    }

    @Override
    public boolean perform() {
        final int count = inputPollers.length;
        for (int i = 0; i < count; i++) {
            final int index = getAndIncrementRoundRobinIndex(count);
            if (inputPollers[index].poll(receiver) > 0) {
                return true;
            }
        }
        return false;
    }

    private int getAndIncrementRoundRobinIndex(final int count) {
        final int index = roundRobinIndex;
        roundRobinIndex++;
        if (roundRobinIndex >= count) {
            roundRobinIndex = 0;
        }
        return index;
    }

    private Input.Poller[] initPollersFor(final Input... inputs) {
        final Input.Poller[] pollers = new Input.Poller[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            pollers[i] = inputs[i].poller();
        }
        return pollers;
    }
}
