
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

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import static org.tools4j.elara.flyweight.FrameType.COMMAND_TYPE;

/**
 * A flyweight that can read either of {@link CommandFrame} and {@link EventFrame}.
 */
public class FlyweightDataFrame implements Flyweight<FlyweightDataFrame>, DataFrame {
    private static final DirectBuffer EMPTY = new UnsafeBuffer(0, 0);
    private final FlyweightHeader header = new FlyweightHeader(FrameDescriptor.HEADER_LENGTH);
    private final FlyweightCommand command = new FlyweightCommand();
    private final FlyweightEvent event = new FlyweightEvent();

    @Override
    public FlyweightDataFrame wrap(final DirectBuffer buffer, final int offset) {
        header.wrap(buffer, offset);
        if (header.type() == COMMAND_TYPE) {
            command.wrap(buffer, offset);
            event.reset();
        } else {
            command.reset();
            event.wrap(buffer, offset);
        }
        return this;
    }

    @Override
    public boolean valid() {
        return command.valid() || event.valid();
    }

    @Override
    public int headerLength() {
        return command.valid() ? command.headerLength() : event.valid() ? event.headerLength() : header.headerLength();
    }

    public FlyweightDataFrame reset() {
        header.reset();
        command.reset();
        event.reset();
        return this;
    }

    @Override
    public Header header() {
        return command.valid() ? command.header() : event.valid() ? event.header() : header;
    }

    @Override
    public int sourceId() {
        return command.valid() ? command.sourceId() : event.valid() ? event.sourceId() : 0;
    }

    @Override
    public long sourceSequence() {
        return command.valid() ? command.sourceSequence() : event.valid() ? event.sourceSequence() : 0;
    }

    public int index() {
        return event.valid() ? event.index() : 0;
    }

    public boolean last() {
        return event.valid() && event.last();
    }


    public long eventSequence() {
        return event.valid() ? event.eventSequence() : 0;
    }

    @Override
    public int payloadType() {
        return command.valid() ? command.payloadType() : event.valid() ? event.payloadType() : 0;
    }

    public long eventTime() {
        return event.valid() ? event.eventTime() : 0;
    }
    @Override
    public DirectBuffer payload() {
        return command.valid() ? command.payload() : event.valid() ? event.payload() : EMPTY;
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        if (command.valid()) {
            return command.writeTo(dst, dstOffset);
        }
        if (event.valid()) {
            return event.writeTo(dst, dstOffset);
        }
        return 0;
    }
    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        if (command.valid()) {
            return command.printTo(dst);
        }
        if (event.valid()) {
            return event.printTo(dst);
        }
        return dst.append("FlyweightDataFrame{???}");
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(256)).toString();
    }

}
