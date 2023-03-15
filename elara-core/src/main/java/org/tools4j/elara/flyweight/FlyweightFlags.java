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
package org.tools4j.elara.flyweight;

import org.tools4j.elara.event.Event;

import static java.util.Objects.requireNonNull;
import static org.tools4j.elara.flyweight.FrameType.EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.NIL_EVENT_TYPE;
import static org.tools4j.elara.flyweight.FrameType.ROLLBACK_EVENT_TYPE;

final class FlyweightFlags implements Event.Flags {
    private final EventFrame frame;

    public FlyweightFlags(final EventFrame frame) {
        this.frame = requireNonNull(frame);
    }

    @Override
    public char value() {
        switch (frame.type()) {
            case EVENT_TYPE:
                return isLast() ? COMMIT : NONE;
            case NIL_EVENT_TYPE:
                assert isLast();
                return NIL;
            case ROLLBACK_EVENT_TYPE:
                assert isLast();
                return ROLLBACK;
        }
        return NONE;
    }

    @Override
    public boolean isLast() {
        return frame.last();
    }

    @Override
    public String toString() {
        return "flags=" + (frame.valid() ? value() : "?");
    }

}
