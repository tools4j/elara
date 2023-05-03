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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.app.handler.CommandTracker;
import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.input.SingleSourceInput;
import org.tools4j.elara.send.CommandSender;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.boot.BootCommands.BOOT_NOTIFY_APP_START;
import static org.tools4j.elara.stream.SendingResult.SENT;

public final class BootCommandInput implements SingleSourceInput {
    private final BootPlugin bootPlugin;
    private final BaseState baseState;
    private boolean commandSent;

    BootCommandInput(final BootPlugin bootPlugin, final BaseState baseState) {
        this.bootPlugin = requireNonNull(bootPlugin);
        this.baseState = requireNonNull(baseState);
    }

    @Override
    public int poll(final CommandSender sender, final CommandTracker commandTracker) {
        if (commandSent) {
            return 0;
        }
        //NOTE: make sure that command is not de-duplicated
        final long nextSourceSeq = 1 + baseState.lastAppliedCommandSequence(sender.sourceId());
        commandTracker.transientCommandState().sourceSequenceGenerator().nextSequence(nextSourceSeq);

        //send command now
        if (SENT == sender.sendCommandWithoutPayload(BOOT_NOTIFY_APP_START)) {
            commandSent = true;
            bootPlugin.notifyCommandSent(commandTracker.transientCommandState());
        }
        return 1;
    }
}
