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
package org.tools4j.elara.send;

import org.tools4j.elara.app.state.TransientInFlightState;
import org.tools4j.elara.source.CommandSourceProvider;

import static java.util.Objects.requireNonNull;

public class DefaultCommandContext implements CommandContext {

    private final TransientInFlightState transientInFlightState;
    private final CommandSourceProvider commandSourceProvider;

    public DefaultCommandContext(final TransientInFlightState transientInFlightState,
                                 final CommandSourceProvider commandSourceProvider) {
        this.transientInFlightState = requireNonNull(transientInFlightState);
        this.commandSourceProvider = requireNonNull(commandSourceProvider);
    }

    @Override
    public TransientInFlightState transientInFlightState() {
        return transientInFlightState;
    }

    @Override
    public CommandSourceProvider commandSources() {
        return commandSourceProvider;
    }

    @Override
    public String toString() {
        return "DefaultCommandContext" +
                ":has-in-flight-cmd=" + hasInFlightCommand() +
                "|transient-in-flight-state=" + transientInFlightState +
                "|cmd-sources=" + commandSourceProvider;
    }
}
