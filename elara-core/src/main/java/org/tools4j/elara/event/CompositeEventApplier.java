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
package org.tools4j.elara.event;

import org.tools4j.elara.app.handler.EventApplier;
import org.tools4j.elara.app.state.ThinEventApplier;
import org.tools4j.elara.flyweight.EventType;

import static java.util.Objects.requireNonNull;

public class CompositeEventApplier implements EventApplier {

    private final EventApplier[] appliers;

    private CompositeEventApplier(final EventApplier... appliers) {
        this.appliers = requireNonNull(appliers);
    }

    public static EventApplier create(final EventApplier applier1, EventApplier applier2) {
        if (applier1 == EventApplier.NOOP) {
            return applier2;
        }
        if (applier2 == EventApplier.NOOP) {
            return applier1;
        }
        if (applier1 instanceof ThinEventApplier && applier2 instanceof ThinEventApplier) {
            return new CompositeThinEventApplier((ThinEventApplier)applier1, (ThinEventApplier)applier2);
        }
        return new CompositeEventApplier(applier1, applier2);
    }

    public static EventApplier create(final EventApplier... appliers) {
        boolean allThin = true;
        int count = 0;
        for (int i = 0; i < appliers.length; i++) {
            count += (appliers[i] == NOOP ? 0 : 1);
            allThin = appliers[i] instanceof ThinEventApplier;
        }
        if (count == 0) {
            return NOOP;
        }
        if (count == 1) {
            for (int i = 0; i < appliers.length; i++) {
                if (appliers[i] != NOOP) {
                    return appliers[i];
                }
            }
            throw new InternalError("Should have exactly 1 non-NOOP applier");
        }
        final EventApplier[] components = allThin ? new ThinEventApplier[count] : new EventApplier[count];
        int index = 0;
        for (int i = 0; i < appliers.length; i++) {
            if (appliers[i] != NOOP) {
                components[index] = appliers[i];
                index++;
            }
        }
        assert index == count;
        return allThin ? new CompositeThinEventApplier((ThinEventApplier[])components)
                : new CompositeEventApplier(components);
    }

    @Override
    public void onEvent(final Event event) {
        for (final EventApplier applier : appliers) {
            applier.onEvent(event);
        }
    }

    private static final class CompositeThinEventApplier implements ThinEventApplier {
        private final ThinEventApplier[] appliers;
        public CompositeThinEventApplier(final ThinEventApplier... appliers) {
            this.appliers = requireNonNull(appliers);
        }

        @Override
        public void onEvent(final int srcId, final long srcSeq, final long evtSeq, final int index, final EventType evtType, final long evtTime, final int payloadType) {
            for (final ThinEventApplier applier : appliers) {
                applier.onEvent(srcId, srcSeq, evtSeq, index, evtType, evtTime, payloadType);
            }
        }
    }

}
