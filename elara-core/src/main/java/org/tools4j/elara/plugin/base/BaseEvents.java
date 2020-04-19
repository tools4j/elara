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
package org.tools4j.elara.plugin.base;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.EventType;
import org.tools4j.elara.flyweight.FlyweightEvent;

public enum BaseEvents {
    ;
    private static final DirectBuffer EMPTY_BUFFER = new UnsafeBuffer(0, 0);

    public static FlyweightEvent commit(final FlyweightEvent flyweightEvent,
                                        final MutableDirectBuffer headerBuffer,
                                        final int offset,
                                        final Command command,
                                        final short index) {
        return empty(flyweightEvent, headerBuffer, offset, command, index,
                EventType.COMMIT);
    }

    public static FlyweightEvent rollback(final FlyweightEvent flyweightEvent,
                                          final MutableDirectBuffer headerBuffer,
                                          final int offset,
                                          final Command command,
                                          final short index) {
        return empty(flyweightEvent, headerBuffer, offset, command, index,
                EventType.ROLLBACK);
    }

    public static FlyweightEvent empty(final FlyweightEvent flyweightEvent,
                                       final MutableDirectBuffer headerBuffer,
                                       final int offset,
                                       final Command command,
                                       final short index,
                                       final int type) {
        return flyweightEvent.init(headerBuffer, offset, command.id().input(), command.id().sequence(), index,
                type, command.time(), EMPTY_BUFFER, 0, 0);
    }
}
