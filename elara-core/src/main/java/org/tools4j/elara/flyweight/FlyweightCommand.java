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
package org.tools4j.elara.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.command.Command;

public class FlyweightCommand implements Flyweight<FlyweightCommand>, Command, Command.Id, DataFrame {

    public static final short INDEX = Short.MIN_VALUE;
    private final FlyweightDataFrame frame = new FlyweightDataFrame();

    public FlyweightCommand init(final MutableDirectBuffer header,
                                 final int headerOffset,
                                 final int source,
                                 final long sequence,
                                 final int type,
                                 final long time,
                                 final DirectBuffer payload,
                                 final int payloadOffset,
                                 final int payloadSize) {
        frame.init(header, headerOffset, source, type, sequence, time, Flags.NONE, INDEX, payload, payloadOffset, payloadSize);
        return this;
    }

    public FlyweightCommand init(final DirectBuffer header,
                                 final int headerOffset,
                                 final DirectBuffer payload,
                                 final int payloadOffset,
                                 final int payloadSize) {
        frame.init(header, headerOffset, payload, payloadOffset, payloadSize);
        return this;
    }

    @Override
    public FlyweightCommand init(final DirectBuffer command, final int offset) {
        frame.init(command, offset);
        return this;
    }

    public boolean valid() {
        return frame.valid();
    }

    public FlyweightCommand reset() {
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
    public int source() {
        return header().source();
    }

    @Override
    public long sequence() {
        return header().sequence();
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
    public int writeTo(final MutableDirectBuffer buffer, final int offset) {
        return frame.writeTo(buffer, offset);
    }

    @Override
    public String toString() {
        return valid() ? "FlyweightCommand{" +
                "source=" + source() +
                ", type=" + type() +
                ", sequence=" + sequence() +
                ", time=" + time() +
                ", version=" + header().version() +
                ", payload-size=" + header().payloadSize() +
                '}' : "FlyweightCommand";
    }
}
