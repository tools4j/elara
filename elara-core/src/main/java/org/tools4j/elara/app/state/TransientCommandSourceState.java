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
package org.tools4j.elara.app.state;

import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.sequence.SequenceGenerator;
import org.tools4j.elara.source.CommandSource;

/**
 * Transient state associated with command sending from a particular command source.  Transient state is not (yet)
 * reflected by events and therefore non-deterministic.  Transient command state is used by a {@link CommandSource} and
 * by the {@link CommandContext} to determine if commands are currently in-flight, meaning that some events are still
 * missing for commands that have been sent.
 * <p>
 * Note however that transient state should not be used in the decision-making logic of the application, otherwise its
 * state will not be deterministic and cannot be reproduced through event replay.
 *
 * @see CommandSource#transientCommandSourceState()
 * @see CommandSource#hasInFlightCommand()
 * @see CommandContext#hasInFlightCommand()
 */
public interface TransientCommandSourceState extends TransientState {
    long NIL_SEQUENCE = SequenceGenerator.NIL_SEQUENCE;

    /**
     * Returns the source ID for commands associated with this command state.
     * @return the source ID for commands
     */
    int sourceId();

    /**
     * Returns the command sequence generator for commands associated with this command state.
     * @return the command sequence generator incremented for every command sent
     * @see #sourceSequenceOfLastSentCommand()
     */
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
     * Returns the maximum source sequence number known to exist in the engine for this source ID, possibly not yet
     * received by this application. Returns {@link BaseState#NIL_SEQUENCE} if nothing is known about the max available
     * source sequence, or no events exist yet for this source ID.
     *
     * @return  the maximum source sequence know to exist for this source ID, or {@link BaseState#NIL_SEQUENCE} if
     *          unavailable or unknown
     * @see TransientEngineState#maxAvailableSourceSequence(int)
     */
    long maxAvailableSourceSequence();

    /**
     * Returns the sending time of the most recently sent command, or
     * {@link org.tools4j.elara.time.TimeSource#MIN_VALUE TimeSource.MIN_VALUE} if no command was ever sent yet.
     * @return the sending time of the command
     * @see #sourceSequenceGenerator()
     */
    long sendingTimeOfLastSentCommand();
}
