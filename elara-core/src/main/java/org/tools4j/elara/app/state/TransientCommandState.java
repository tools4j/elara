/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2023 tools4j.org (Marco Terzer, Anton Anufriev)
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
package org.tools4j.elara.app.state;

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.sequence.SequenceGenerator;

/**
 * Transient state associated with command sending that is not reflected by events, hence it is non-deterministic.
 * Transient command state is used by {@link CommandTracker} to determine if commands are currently
 * {@link CommandTracker#hasInFlightCommand() in-flight}, meaning that some events are still missing for commands that
 * have been sent.
 * <p>
 * Note however that transient state should not be used in the decision-making logic of the application, otherwise its
 * state will not be deterministic and cannot be reproduced through event replay.
 */
public interface TransientCommandState {
    long NIL_SEQUENCE = SequenceGenerator.NIL_SEQUENCE;
    int sourceId();
    SequenceGenerator sourceSequenceGenerator();

    /**
     * Returns the number of commands sent from the source associated with this command state.
     * @return the number of commands sent
     */
    long commandsSent();

    /**
     * Returns the source sequence of the most recently sent command, or {@link #NIL_SEQUENCE} if no command was
     * ever sent yet.
     * @return the source sequence of the command, or {@link #NIL_SEQUENCE} if unavailable
     */
    long sourceSequenceOfLastSentCommand();

    /**
     * Returns the sending time of the most recently sent command, or
     * {@link org.tools4j.elara.time.TimeSource#MIN_VALUE TimeSource.MIN_VALUE} if no command was ever sent yet.
     * @return the sending time of the command
     */
    long sendingTimeOfLastSentCommand();
}
