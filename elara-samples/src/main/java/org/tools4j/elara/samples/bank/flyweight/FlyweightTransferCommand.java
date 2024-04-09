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
import org.tools4j.elara.app.message.Command;
import org.tools4j.elara.logging.Printable;
import org.tools4j.elara.samples.bank.command.TransferCommand.MutableTransferCommand;

public class FlyweightTransferCommand implements MutableTransferCommand, FlyweightBankCommand {

    private static final int AMOUNT_OFFSET = 0;
    private static final int FROM_LENGTH_OFFSET = 8;
    private static final int TO_LENGTH_OFFSET = 12;
    private static final int VARIABLE_VALUE_OFFSET = 16;//from, then to
    private DirectBuffer buffer;
    private MutableDirectBuffer encodingBuffer;
    private int offset;
    private final AsciiString from = new AsciiString();
    private final AsciiString to = new AsciiString();

    @Override
    public FlyweightTransferCommand reset() {
        buffer = null;
        encodingBuffer = null;
        offset = 0;
        return this;
    }

    @Override
    public FlyweightTransferCommand wrap(final Command command) {
        if (command.payloadType() != type().value) {
            throw new IllegalArgumentException("Expected " + type() + "[" + type().value + "]" +
                    " but found command with payload type=" + command.payloadType());
        }
        this.buffer = command.payload();
        this.encodingBuffer = null;
        this.offset = 0;
        return this;
    }

    @Override
    public FlyweightTransferCommand wrap(final MutableDirectBuffer buffer, final int offset) {
        this.buffer = buffer;
        this.encodingBuffer = buffer;
        this.offset = offset;
        buffer.putInt(offset + FROM_LENGTH_OFFSET, 0);//empty account name as default
        buffer.putInt(offset + TO_LENGTH_OFFSET, 0);//empty account name as default
        return this;
    }

    @Override
    public int encodeTo(final MutableDirectBuffer dstBuffer, final int dstOffset) {
        final int length = encodingLength();
        buffer.getBytes(offset, dstBuffer, dstOffset, length);
        return length;
    }

    @Override
    public CharSequence from() {
        final int length = buffer.getInt(offset + FROM_LENGTH_OFFSET);
        from.wrap(buffer, offset + VARIABLE_VALUE_OFFSET, length);
        return from;
    }

    @Override
    public CharSequence to() {
        final int fromLength = buffer.getInt(offset + FROM_LENGTH_OFFSET);
        final int toLength = buffer.getInt(offset + TO_LENGTH_OFFSET);
        to.wrap(buffer, offset + VARIABLE_VALUE_OFFSET + fromLength, toLength);
        return to;
    }

    @Override
    public double amount() {
        return buffer.getDouble(offset + AMOUNT_OFFSET);
    }

    @Override
    public FlyweightTransferCommand from(final CharSequence from) {
        final int fromLength = buffer.getInt(offset + FROM_LENGTH_OFFSET);
        final int toLength = buffer.getInt(offset + TO_LENGTH_OFFSET);
        if (toLength != 0 && fromLength != from.length()) {
            //move to value to new position
            encodingBuffer.putBytes(offset + VARIABLE_VALUE_OFFSET + from.length(),
                    encodingBuffer, offset + VARIABLE_VALUE_OFFSET + fromLength, toLength);
        }
        encodingBuffer.putInt(offset + FROM_LENGTH_OFFSET, from.length());
        encodingBuffer.putStringWithoutLengthAscii(offset + VARIABLE_VALUE_OFFSET, from);
        return this;
    }

    @Override
    public FlyweightTransferCommand to(final CharSequence to) {
        final int fromLength = buffer.getInt(offset + FROM_LENGTH_OFFSET);
        encodingBuffer.putInt(offset + TO_LENGTH_OFFSET, to.length());
        encodingBuffer.putStringWithoutLengthAscii(offset + VARIABLE_VALUE_OFFSET + fromLength, to);
        return this;
    }

    @Override
    public FlyweightTransferCommand amount(final double amount) {
        encodingBuffer.putDouble(offset + AMOUNT_OFFSET, amount);
        return this;
    }

    @Override
    public int encodingLength() {
        return 16 + buffer.getInt(offset + FROM_LENGTH_OFFSET) + buffer.getInt(offset + TO_LENGTH_OFFSET);
    }

    @Override
    public String toString() {
        return Printable.toString(this);
    }

    @Override
    public StringBuilder printTo(final StringBuilder builder) {
        return builder.append("TransferCommand")
                .append(":from=").append(from())
                .append("|to=").append(to())
                .append("|amount=").append(amount());
    }
}
