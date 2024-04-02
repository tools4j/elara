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
package org.tools4j.elara.samples.bank.flyweight;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.logging.Printable;
import org.tools4j.elara.samples.bank.event.AccountCreatedEvent.MutableAccountCreatedEvent;

public class FlyweightAccountCreatedEvent implements MutableAccountCreatedEvent, FlyweightBankEvent {

    private static final int NAME_LENGTH_OFFSET = 0;
    private static final int NAME_VALUE_OFFSET = 4;
    private DirectBuffer buffer;
    private MutableDirectBuffer encodingBuffer;
    private int offset;
    private final AsciiString name = new AsciiString();

    @Override
    public FlyweightAccountCreatedEvent reset() {
        buffer = null;
        encodingBuffer = null;
        offset = 0;
        return this;
    }

    @Override
    public FlyweightAccountCreatedEvent wrap(final Event event) {
        if (event.payloadType() != type().value) {
            throw new IllegalArgumentException("Expected " + type() + "[" + type().value + "]" +
                    " but found event with payload type=" + event.payloadType());
        }
        this.buffer = event.payload();
        this.encodingBuffer = null;
        this.offset = 0;
        return this;
    }

    @Override
    public FlyweightAccountCreatedEvent wrap(final MutableDirectBuffer buffer, final int offset) {
        this.buffer = buffer;
        this.encodingBuffer = buffer;
        this.offset = offset;
        buffer.putInt(offset + NAME_LENGTH_OFFSET, 0);//empty account name as default
        return this;
    }

    @Override
    public CharSequence name() {
        final int length = buffer.getInt(offset + NAME_LENGTH_OFFSET);
        name.wrap(buffer, offset + NAME_VALUE_OFFSET, length);
        return name;
    }

    @Override
    public FlyweightAccountCreatedEvent name(final CharSequence account) {
        encodingBuffer.putInt(offset + NAME_LENGTH_OFFSET, account.length());
        encodingBuffer.putStringWithoutLengthAscii(offset + NAME_VALUE_OFFSET, account);
        return this;
    }

    @Override
    public int encodingLength() {
        return 4 + buffer.getInt(offset + NAME_LENGTH_OFFSET);
    }

    @Override
    public String toString() {
        return Printable.toString(this);
    }

    @Override
    public StringBuilder printTo(final StringBuilder builder) {
        return builder.append("AccountCreatedEvent")
                .append(":name=").append(name());
    }
}
