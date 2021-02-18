/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 tools4j.org (Marco Terzer, Anton Anufriev)
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

import org.tools4j.elara.event.Event;
import org.tools4j.elara.output.CommandLoopback;
import org.tools4j.elara.output.Output;

import static org.tools4j.elara.plugin.boot.BootCommands.SIGNAL_APP_INITIALISATION_COMPLETE;
import static org.tools4j.elara.plugin.boot.BootEvents.APP_INITIALISATION_STARTED;

public class BootOutput implements Output {

    @Override
    public Ack publish(final Event event, final boolean replay, final int retry, final CommandLoopback loopback) {
        if (!replay && retry == 0 && event.type() == APP_INITIALISATION_STARTED) {
            loopback.enqueueCommandWithoutPayload(SIGNAL_APP_INITIALISATION_COMPLETE);
            return Ack.COMMIT;
        }
        return Ack.IGNORED;
    }
}
