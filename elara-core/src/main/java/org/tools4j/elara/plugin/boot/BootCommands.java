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

import org.tools4j.elara.command.Command;
import org.tools4j.elara.flyweight.DataFrame;

import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;

/**
 * Boot commands added to the command store when booting an elara application to signal startup and initialisation of
 * the application.
 */
public enum BootCommands {
    ;
    /**
     * Command sent once when an application is started right after applying all replayed eents;  the boot plugin
     * command processor translates this command into an {@link BootEvents#BOOT_APP_STARTED BOOT_APP_STARTED} event.
     */
    public static final int BOOT_NOTIFY_APP_START = -20;

    public static boolean isBootCommand(final Command command) {
        return isBootCommand(command.payloadType());
    }

    public static boolean isBootCommand(final DataFrame frame) {
        return frame.header().type() == COMMAND_TYPE && isBootCommand(frame.payloadType());
    }

    public static boolean isBootCommand(final int payloadType) {
        return payloadType == BOOT_NOTIFY_APP_START;
    }

    public static String bootCommandName(final Command command) {
        return bootCommandName(command.payloadType());
    }

    public static String bootCommandName(final DataFrame frame) {
        return bootCommandName(frame.payloadType());
    }

    public static String bootCommandName(final int payloadType) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payloadType) {
            case BOOT_NOTIFY_APP_START:
                return "BOOT_NOTIFY_APP_START";
            default:
                throw new IllegalArgumentException("Not a boot command type: " + payloadType);
        }
    }
}
