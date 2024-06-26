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
package org.tools4j.elara.plugin.boot;

import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.handler.EventProcessor;
import org.tools4j.elara.app.message.Event;
import org.tools4j.elara.send.CommandContext;
import org.tools4j.elara.send.CommandSender;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.plugin.boot.BootEvents.BOOT_APP_STARTED;

final class BootEventApplier implements EventApplier, EventProcessor {

    private final BootPlugin bootPlugin;

    BootEventApplier(final BootPlugin bootPlugin) {
        this.bootPlugin = requireNonNull(bootPlugin);
    }

    @Override
    public void onEvent(final Event event) {
        if (event.payloadType() == BOOT_APP_STARTED) {
            if (event.sourceId() == bootPlugin.sourceId() && bootPlugin.bootCommandSourceSequence() == event.sourceSequence()) {
                bootPlugin.notifyEventReceived(event);
            }
        }
    }

    @Override
    public void onEvent(final Event event, final CommandContext commandContext, final CommandSender sender) {
        onEvent(event);
    }
}
