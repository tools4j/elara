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
package org.tools4j.elara.samples.bank;

import org.tools4j.elara.command.Command;
import org.tools4j.elara.event.Event;
import org.tools4j.elara.logging.Printable;
import org.tools4j.elara.samples.bank.command.CommandType;
import org.tools4j.elara.samples.bank.event.EventType;
import org.tools4j.elara.samples.bank.flyweight.BankFlyweight;
import org.tools4j.elara.samples.bank.flyweight.FlyweightAccountCreatedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightAccountCreationRejectedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightAmountAddedOrRemovedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightCreateAccountCommand;
import org.tools4j.elara.samples.bank.flyweight.FlyweightDepositCommand;
import org.tools4j.elara.samples.bank.flyweight.FlyweightTransactionRejectedEvent;
import org.tools4j.elara.samples.bank.flyweight.FlyweightTransferCommand;
import org.tools4j.elara.samples.bank.flyweight.FlyweightWithdrawCommand;

import java.io.PrintStream;

final class PayloadPrinter {
    private final StringBuilder stringBuilder = new StringBuilder(256);
    private final FlyweightCreateAccountCommand createAccountCommand = new FlyweightCreateAccountCommand();
    private final FlyweightDepositCommand depositCommand = new FlyweightDepositCommand();
    private final FlyweightWithdrawCommand withdrawCommand = new FlyweightWithdrawCommand();
    private final FlyweightTransferCommand transferCommand = new FlyweightTransferCommand();
    private final FlyweightAccountCreatedEvent accountCreatedEvent = new FlyweightAccountCreatedEvent();
    private final FlyweightAccountCreationRejectedEvent accountCreationRejectedEvent = new FlyweightAccountCreationRejectedEvent();
    private final FlyweightAmountAddedOrRemovedEvent amountAddedOrRemovedEvent = new FlyweightAmountAddedOrRemovedEvent();
    private final FlyweightTransactionRejectedEvent transactionRejectedEvent = new FlyweightTransactionRejectedEvent();

    public PayloadPrinter reset() {
        stringBuilder.setLength(0);
        return this;
    }

    public PayloadPrinter append(final CharSequence s) {
        stringBuilder.append(s);
        return this;
    }

    public PayloadPrinter append(final double d) {
        stringBuilder.append(d);
        return this;
    }

    public PayloadPrinter append(final int i) {
        stringBuilder.append(i);
        return this;
    }

    public PayloadPrinter append(final boolean b) {
        stringBuilder.append(b);
        return this;
    }

    public PayloadPrinter append(final Printable printable) {
        printable.printTo(stringBuilder);
        return this;
    }

    public PayloadPrinter append(final BankFlyweight flyweight) {
        flyweight.printTo(stringBuilder);
        return this;
    }

    public PayloadPrinter append(final Command command) {
        append("command=").append((Printable)command)
                .append(", payload=");
        if (command.isApplication()) {
            switch (CommandType.byValue(command.payloadType())) {
                case CreateAccount:
                    return append(createAccountCommand.wrap(command));
                case Deposit:
                    return append(depositCommand.wrap(command));
                case Withdraw:
                    return append(withdrawCommand.wrap(command));
                case Transfer:
                    return append(transferCommand.wrap(command));
                default:
                    //fallthrough
            }
        }
        return append("(unknown)");
    }

    public PayloadPrinter append(final Event event) {
        append("event=").append((Printable)event)
                .append(", payload=");
        if (event.isApplication()) {
            switch (EventType.byValue(event.payloadType())) {
                case AccountCreated:
                    return append(accountCreatedEvent.wrap(event));
                case AccountCreationRejected:
                    return append(accountCreationRejectedEvent.wrap(event));
                case AmountAddedOrRemoved:
                    return append(amountAddedOrRemovedEvent.wrap(event));
                case TransactionRejected:
                    return append(transactionRejectedEvent.wrap(event));
                default:
                    //fallthrough
            }
        }
        return append("(unknown)");
    }

    public void println() {
        println(System.out);
    }

    public void println(final PrintStream stream) {
        stream.println(stringBuilder);
    }
}
