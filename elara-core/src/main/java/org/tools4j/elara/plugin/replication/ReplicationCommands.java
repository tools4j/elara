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
package org.tools4j.elara.plugin.replication;

import org.tools4j.elara.command.Command;
import org.tools4j.elara.plugin.boot.BootEvents;

/**
 * Boot commands added to the command log when booting an elara application to signal startup and initialisation of the
 * application.
 */
public enum ReplicationCommands {
    ;
    public static final int COMMAND_DELEGATED = -99;

    /**
     * Command enqueued to the command via loopback when the first non-replayed event observed;  the boot plugin command
     * processor translates this command into an
     * {@link BootEvents#APP_INITIALISATION_COMPLETED APP_INITIALISATION_COMPLETED} event.
     */
    public static final int SIGNAL_APP_INITIALISATION_COMPLETE = -21;

    public static boolean isBootCommand(final Command command) {
        switch (command.type()) {
            case COMMAND_DELEGATED://fallthrough
                return true;
            default:
                return false;
        }
    }
}
