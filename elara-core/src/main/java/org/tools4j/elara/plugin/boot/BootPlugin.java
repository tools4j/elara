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

import org.tools4j.elara.app.state.BaseState;
import org.tools4j.elara.app.state.TransientCommandState;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.plugin.api.PluginStateProvider.NullState;
import org.tools4j.elara.plugin.api.SystemPlugin;
import org.tools4j.elara.time.TimeSource;

/**
 * A plugin that issues commands and events related to booting an elara application to indicate that the application has
 * been started and initialised.
 */
public class BootPlugin implements SystemPlugin<NullState> {

    public static final int DEFAULT_SOURCE_ID = -20;
    public static final BootPlugin DEFAULT = new BootPlugin(DEFAULT_SOURCE_ID);

    private final int sourceId;
    private final BootPluginSpecification specification = new BootPluginSpecification(this);

    private long bootCommandSourceSequence = BaseState.NIL_SEQUENCE;
    private long bootCommandSendingTime = TimeSource.MIN_VALUE;
    private long bootEventSequence = BaseState.NIL_SEQUENCE;
    private long bootEventTime = TimeSource.MIN_VALUE;

    public BootPlugin(final int sourceId) {
        this.sourceId = sourceId;
    }

    void notifyCommandSent(final TransientCommandState transientCommandState) {
        this.bootCommandSourceSequence = transientCommandState.sourceSequenceOfLastSentCommand();
        this.bootCommandSendingTime = transientCommandState.sendingTimeOfLastSentCommand();
    }

    void notifyEventReceived(final Event event) {
        this.bootEventSequence = event.eventSequence();
        this.bootCommandSendingTime = event.eventTime();
    }

    public int sourceId() {
        return sourceId;
    }

    public long bootCommandSourceSequence() {
        return bootCommandSourceSequence;

    }

    public long bootCommandSendingTime() {
        return bootCommandSendingTime;
    }

    public long bootEventSequence() {
        return bootEventSequence;
    }

    public long bootEventTime() {
        return bootEventTime;
    }

    public boolean isBootComplete() {
        return bootEventSequence != BaseState.NIL_SEQUENCE;
    }

    @Override
    public SystemPluginSpecification<NullState> specification() {
        return specification;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BootPlugin that = (BootPlugin) o;
        return sourceId == that.sourceId;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode() + sourceId;
    }

    @Override
    public String toString() {
        return "BootPlugin" +
                ":sourceId=" + sourceId +
                "|boot-cmd-source-seq=" + bootCommandSourceSequence +
                "|boot-cmd-sending-time=" + bootCommandSendingTime +
                "|boot-evt-seq=" + bootEventSequence +
                "|boot-evt-time=" + bootEventTime +
                "|boot-complete=" + isBootComplete() +
                '}';
    }
}