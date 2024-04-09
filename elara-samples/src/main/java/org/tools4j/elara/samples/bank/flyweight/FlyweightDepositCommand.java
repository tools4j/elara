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
import org.tools4j.elara.samples.bank.command.DepositCommand.MutableDepositCommand;

public class FlyweightDepositCommand implements MutableDepositCommand, FlyweightBankCommand {

    private static final int AMOUNT_OFFSET = 0;
    private static final int ACCOUNT_LENGTH_OFFSET = 8;
    private static final int ACCOUNT_VALUE_OFFSET = 12;
    private DirectBuffer buffer;
    private MutableDirectBuffer encodingBuffer;
    private int offset;
    private final AsciiString account = new AsciiString();

    @Override
    public FlyweightDepositCommand reset() {
        buffer = null;
        encodingBuffer = null;
        offset = 0;
        return this;
    }

    @Override
    public FlyweightDepositCommand wrap(final Command command) {
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
    public FlyweightDepositCommand wrap(final MutableDirectBuffer buffer, final int offset) {
        this.buffer = buffer;
        this.encodingBuffer = buffer;
        this.offset = offset;
        buffer.putInt(offset + ACCOUNT_LENGTH_OFFSET, 0);//empty account name as default
        return this;
    }

    @Override
    public int encodeTo(final MutableDirectBuffer dstBuffer, final int dstOffset) {
        final int length = encodingLength();
        buffer.getBytes(offset, dstBuffer, dstOffset, length);
        return length;
    }

    @Override
    public CharSequence account() {
        final int length = buffer.getInt(offset + ACCOUNT_LENGTH_OFFSET);
        account.wrap(buffer, offset + ACCOUNT_VALUE_OFFSET, length);
        return account;
    }

    @Override
    public double amount() {
        return buffer.getDouble(offset + AMOUNT_OFFSET);
    }

    @Override
    public FlyweightDepositCommand account(final CharSequence account) {
        encodingBuffer.putInt(offset + ACCOUNT_LENGTH_OFFSET, account.length());
        encodingBuffer.putStringWithoutLengthAscii(offset + ACCOUNT_VALUE_OFFSET, account);
        return this;
    }

    @Override
    public FlyweightDepositCommand amount(final double amount) {
        encodingBuffer.putDouble(offset + AMOUNT_OFFSET, amount);
        return this;
    }

    @Override
    public int encodingLength() {
        return 12 + buffer.getInt(offset + ACCOUNT_LENGTH_OFFSET);
    }

    @Override
    public String toString() {
        return Printable.toString(this);
    }

    @Override
    public StringBuilder printTo(final StringBuilder builder) {
        return builder.append("DepositCommand")
                .append(":account=").append(account())
                .append("|amount=").append(amount());
    }
}
