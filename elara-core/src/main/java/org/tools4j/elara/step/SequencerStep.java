/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.step;

import org.tools4j.elara.input.Input;
import org.tools4j.elara.send.SenderSupplier;

import static java.util.Objects.requireNonNull;

/**
 * Polls all inputs, sequences received messages and send them as commands using the command sender.
 * Depending on the command sender type, the command processor is either directly invoked, or the command is appended to
 * the command queue.
 */
public final class SequencerStep implements AgentStep {

    private final SenderSupplier senderSupplier;
    private final Input[] inputs;

    private int roundRobinIndex = 0;

    public SequencerStep(final SenderSupplier senderSupplier, final Input... inputs) {
        this.senderSupplier = requireNonNull(senderSupplier);
        this.inputs = requireNonNull(inputs);
    }

    @Override
    public int doWork() {
        final int count = inputs.length;
        for (int i = 0; i < count; i++) {
            final int index = getAndIncrementRoundRobinIndex(count);
            if (inputs[index].poll(senderSupplier) > 0) {
                return 1;
            }
        }
        return 0;
    }

    private int getAndIncrementRoundRobinIndex(final int count) {
        final int index = roundRobinIndex;
        roundRobinIndex++;
        if (roundRobinIndex >= count) {
            roundRobinIndex = 0;
        }
        return index;
    }
}
