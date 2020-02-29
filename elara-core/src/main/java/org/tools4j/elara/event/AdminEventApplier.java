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
package org.tools4j.elara.event;

import org.tools4j.elara.application.EventApplier;
import org.tools4j.elara.command.CommandLoopback;
import org.tools4j.elara.state.AdminStateProvider;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.event.AdminEvents.*;

public class AdminEventApplier implements EventApplier {

    private final AdminStateProvider.Mutable adminStateProvider;

    public AdminEventApplier(final AdminStateProvider.Mutable adminStateProvider) {
        this.adminStateProvider = requireNonNull(adminStateProvider);
    }

    @Override
    public void onEvent(final Event event, final CommandLoopback loopback) {
        if (event.isAdmin()) {
            onAdminEvent(event, loopback);
        }
    }

    protected void onAdminEvent(final Event event, final CommandLoopback loopback) {
        final int type = event.type();
        if (type == EventType.COMMIT.value()) {
            adminStateProvider.serverState().allEventsAppliedFor(event.id().commandId());
        } else if (type == EventType.TIMER_STARTED.value()) {
            adminStateProvider.timerState().add(timerType(event), timerId(event), timerTimeout(event));
        } else if (type == EventType.TIMER_STOPPED.value()) {
            adminStateProvider.timerState().remove(timerId(event));
        } else if (type == EventType.TIMER_EXPIRED.value()) {
            adminStateProvider.timerState().remove(timerId(event));
        }
    }
}
