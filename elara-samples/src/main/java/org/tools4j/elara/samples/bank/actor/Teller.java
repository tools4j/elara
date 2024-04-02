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
package org.tools4j.elara.samples.bank.actor;

import org.tools4j.elara.app.handler.CommandProcessor;
import org.tools4j.elara.command.Command;
import org.tools4j.elara.route.EventRouter;
import org.tools4j.elara.samples.bank.command.CommandType;
import org.tools4j.elara.samples.bank.flyweight.FlyweightCreateAccountCommand;
import org.tools4j.elara.samples.bank.flyweight.FlyweightDepositCommand;
import org.tools4j.elara.samples.bank.flyweight.FlyweightTransferCommand;
import org.tools4j.elara.samples.bank.flyweight.FlyweightWithdrawCommand;
import org.tools4j.elara.samples.bank.state.Bank;
import org.tools4j.elara.samples.bank.state.BankAccount;
import org.tools4j.elara.samples.bank.state.BankEventRouter;

import static java.util.Objects.requireNonNull;

/**
 * The teller validates commands and sends instructions in form of events to the accountant to execute.
 */
public class Teller implements CommandProcessor {

    private final Bank bank;
    private final FlyweightCreateAccountCommand createAccountCommand = new FlyweightCreateAccountCommand();
    private final FlyweightDepositCommand depositCommand = new FlyweightDepositCommand();
    private final FlyweightWithdrawCommand withdrawCommand = new FlyweightWithdrawCommand();
    private final FlyweightTransferCommand transferCommand = new FlyweightTransferCommand();
    private final BankEventRouter router = new BankEventRouter();

    public Teller(final Bank bank) {
        this.bank = requireNonNull(bank);
    }

    @Override
    public void onCommand(final Command command, final EventRouter router) {
        if (command.isApplication()) {
            final CommandType commandType = CommandType.byValue(command.payloadType());
            onCommand(commandType, command, router);
        }
    }

    private void onCommand(final CommandType type, final Command command, final EventRouter eventRouter) {
        router.init(eventRouter);
        try {
            if (type == CommandType.CreateAccount) {
                bank.createAccount(createAccountCommand.wrap(command), router);
                return;
            }
            final BankAccount account1;
            final BankAccount account2;
            switch (type) {
                case Deposit:
                    depositCommand.wrap(command);
                    account1 = validateAndGetAccount(depositCommand.account());
                    if (account1 != null) {
                        account1.deposit(depositCommand, router);
                    }
                    break;
                case Withdraw:
                    withdrawCommand.wrap(command);
                    account1 = validateAndGetAccount(withdrawCommand.account());
                    if (account1 != null) {
                        account1.withdraw(withdrawCommand, router);
                    }
                    break;
                case Transfer:
                    transferCommand.wrap(command);
                    account1 = validateAndGetAccount(transferCommand.from());
                    account2 = validateAndGetAccount(transferCommand.to());
                    if (account1 != null && account2 != null) {
                        if (account1.transfer(transferCommand, router)) {
                            account2.transfer(transferCommand, router);
                        }
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid command: " + command);
            }
        } finally {
            router.reset();
        }
    }

    private BankAccount validateAndGetAccount(final CharSequence account) {
        if (bank.hasAccount(account)) {
            return bank.account(account);
        }
        router.routeTransactionRejectedEvent(account, "Account does not exist");
        return null;
    }
}
