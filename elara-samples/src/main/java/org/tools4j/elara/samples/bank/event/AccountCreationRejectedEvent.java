/*
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
package org.tools4j.elara.samples.bank.event;

import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;

import static java.util.Objects.requireNonNull;

public class AccountCreationRejectedEvent implements BankEvent {
    public static final EventType TYPE = EventType.AccountCreationRejected;

    public final String account;
    public final String reason;
    public AccountCreationRejectedEvent(final String account, final String reason) {
        this.account = requireNonNull(account);
        this.reason = requireNonNull(reason);
    }

    @Override
    public EventType type() {
        return TYPE;
    }

    @Override
    public DirectBuffer encode() {
        final MutableDirectBuffer buffer = new ExpandableArrayBuffer(
                        + Integer.BYTES + account.length() +
                        + Integer.BYTES + reason.length()
                );
        buffer.putStringAscii(0, account);
        buffer.putStringAscii(4 + account.length(), reason);
        return buffer;
    }

    public String toString() {
        return TYPE + "{account=" + account + ", reason=" + reason + "}";
    }

    public static AccountCreationRejectedEvent decode(final DirectBuffer payload) {
        final String account = payload.getStringAscii(0);
        final String reason = payload.getStringAscii(4 + account.length());
        return new AccountCreationRejectedEvent(account, reason);
    }

    public static String toString(final DirectBuffer payload) {
        return decode(payload).toString();
    }
}
