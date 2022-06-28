/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2022 tools4j.org (Marco Terzer, Anton Anufriev)
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
import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;

public class FlyweightEvent implements Flyweight<FlyweightEvent>, Event, Event.Id, Command.Id, DataFrame {

    private final FlyweightDataFrame frame = new FlyweightDataFrame();
    private final Flags flags = new FlyweightFlags();

    public FlyweightEvent init(final MutableDirectBuffer header,
                               final int headerOffset,
                               final int source,
                               final long sequence,
                               final short index,
                               final int type,
                               final long time,
                               final byte flags,
                               final DirectBuffer payload,
                               final int payloadOffset,
                               final int payloadSize) {
        frame.init(header, headerOffset, source, type, sequence, time, flags, index, payload, payloadOffset, payloadSize);
        return this;
    }

    public FlyweightEvent init(final DirectBuffer header,
                               final int headerOffset,
                               final DirectBuffer payload,
                               final int payloadOffset,
                               final int payloadSize) {
        frame.init(header, headerOffset, payload, payloadOffset, payloadSize);
        return this;
    }

    public FlyweightEvent initSilent(final DirectBuffer header,
                                     final int headerOffset,
                                     final DirectBuffer payload,
                                     final int payloadOffset,
                                     final int payloadSize) {
        frame.initSilent(header, headerOffset, payload, payloadOffset, payloadSize);
        return this;
    }

    @Override
    public FlyweightEvent init(final DirectBuffer event, final int offset) {
        frame.init(event, offset);
        return this;
    }

    public boolean valid() {
        return frame.valid();
    }

    public FlyweightEvent reset() {
        frame.reset();
        return this;
    }

    @Override
    public Header header() {
        return frame.header();
    }

    @Override
    public Id id() {
        return this;
    }

    @Override
    public Command.Id commandId() {
        return this;
    }

    @Override
    public Flags flags() {
        return flags;
    }

    @Override
    public int source() {
        return header().source();
    }

    @Override
    public long sequence() {
        return header().sequence();
    }

    @Override
    public int index() {
        return header().index();
    }

    @Override
    public int type() {
        return header().type();
    }

    @Override
    public long time() {
        return header().time();
    }

    @Override
    public DirectBuffer payload() {
        return frame.payload();
    }

    @Override
    public int writeTo(final MutableDirectBuffer dst, final int dstOffset) {
        return frame.writeTo(dst, dstOffset);
    }

    @Override
    public StringBuilder printTo(final StringBuilder dst) {
        dst.append("FlyweightEvent{");
        if (valid()) {
            final Header header = header();
            dst.append("version=").append(header.version());
            dst.append("|source=").append(header.source());
            dst.append("|sequence=").append(header.sequence());
            dst.append("|index=").append(header.index());
            dst.append("|flags=").append(org.tools4j.elara.flyweight.Flags.toString(header().flags()));
            dst.append("|type=").append(header.type());
            dst.append("|time=").append(header.time());
            dst.append("|payload-size=").append(header.payloadSize());
        } else {
            dst.append("???");
        }
        dst.append('}');
        return dst;
    }

    @Override
    public String toString() {
        return printTo(new StringBuilder(128)).toString();
    }

    private final class FlyweightFlags implements Flags {
        @Override
        public boolean isCommit() {
            return org.tools4j.elara.flyweight.Flags.isCommit(value());
        }

        @Override
        public boolean isRollback() {
            return org.tools4j.elara.flyweight.Flags.isRollback(value());
        }

        @Override
        public boolean isFinal() {
            return org.tools4j.elara.flyweight.Flags.isFinal(value());
        }

        @Override
        public boolean isNonFinal() {
            return org.tools4j.elara.flyweight.Flags.isNonFinal(value());
        }

        @Override
        public byte value() {
            return header().flags();
        }

        @Override
        public String toString() {
            return org.tools4j.elara.flyweight.Flags.toString(value());
        }
    }
}
