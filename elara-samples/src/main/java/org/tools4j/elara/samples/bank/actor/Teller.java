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
package org.tools4j.elara.samples.bank.actor;

import org.agrona.DirectBuffer;
import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.samples.bank.command.CommandType;
import org.tools4j.elara.samples.bank.command.CreateAccountCommand;
import org.tools4j.elara.samples.bank.command.DepositCommand;
import org.tools4j.elara.samples.bank.command.TransferCommand;
import org.tools4j.elara.samples.bank.command.WithdrawCommand;
import org.tools4j.elara.samples.bank.event.BankEvent;
import org.tools4j.elara.samples.bank.event.TransactionRejectedEvent;
import org.tools4j.elara.samples.bank.state.Bank;

import static java.util.Objects.requireNonNull;

public class Teller implements CommandProcessor {

    private final Bank bank;

    public Teller(final Bank bank) {
        this.bank = requireNonNull(bank);
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        if (command.isApplication()) {
            final CommandType commandType = CommandType.byValue(command.type());
            onCommand(commandType, command, router);
        }
    }

    private void onCommand(final CommandType type, final Command command, final EventRouter router) {
        if (type == CommandType.CreateAccount) {
            routeEvent(
                    bank.process(CreateAccountCommand.decode(command.payload())),
                    router
            );
            return;
        }
        final String account;
        switch (type) {
            case Deposit:
                account = DepositCommand.decode(command.payload()).account;
                break;
            case Withdraw:
                account = WithdrawCommand.decode(command.payload()).account;
                break;
            case Transfer:
                account = TransferCommand.decode(command.payload()).from;
                break;
            default:
                throw new IllegalArgumentException("Invalid command: " + command);
        }
        if (!bank.hasAccount(account)) {
            routeEvent(
                    new TransactionRejectedEvent(account, type + " account is invalid"),
                    router
            );
            return;
        }
        final BankEvent[] events = bank.account(account).process(type, command);
        for (final BankEvent event : events) {
            routeEvent(event, router);
        }
    }

    private void routeEvent(final BankEvent event, final EventRouter router) {
        final int type = event.type().value;
        final DirectBuffer encoded = event.encode();
        router.routeEvent(type, encoded, 0, encoded.capacity());
    }
}
