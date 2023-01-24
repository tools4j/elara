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
package org.tools4j.elara.samples.bank.state;

import org.tools4j.elara.command.Command;
import org.tools4j.elara.samples.bank.command.CommandType;
import org.tools4j.elara.samples.bank.command.DepositCommand;
import org.tools4j.elara.samples.bank.command.TransferCommand;
import org.tools4j.elara.samples.bank.command.WithdrawCommand;
import org.tools4j.elara.samples.bank.event.AmountAddedOrRemovedEvent;
import org.tools4j.elara.samples.bank.event.BankEvent;
import org.tools4j.elara.samples.bank.event.TransactionRejectedEvent;

import static java.util.Objects.requireNonNull;

public interface BankAccount {
    Bank bank();
    String name();
    double balance();

    BankEvent[] process(CommandType commandType, Command command);

    interface Mutable extends BankAccount {
        void add(double value);
    }

    class Default implements Mutable {
        private final Bank bank;
        private final String name;
        private double balance;
        public Default(final Bank bank, final String name) {
            this.bank = requireNonNull(bank);
            this.name = requireNonNull(name);
            this.balance = 0;
        }

        @Override
        public Bank bank() {
            return bank;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public double balance() {
            return balance;
        }

        @Override
        public void add(final double value) {
            final double newBalance = balance + value;
            if (Double.isNaN(newBalance) || newBalance < 0) {
                throw new IllegalArgumentException("New balance would be invalid: " + newBalance);
            }
            balance = newBalance;
        }

        @Override
        public BankEvent[] process(final CommandType commandType, final Command command) {
            switch (commandType) {
                case Deposit:
                    return new BankEvent[] {
                            process(DepositCommand.decode(command.payload()))
                    };
                case Withdraw:
                    return new BankEvent[] {
                            process(WithdrawCommand.decode(command.payload()))
                    };
                case Transfer:
                    return process(TransferCommand.decode(command.payload()));
                default:
                    throw new IllegalArgumentException("Unsupported command: " + command);
            }
        }

        public BankEvent process(final DepositCommand command) {
            validateAccount(command.account);
            if (command.amount <= 0) {
                return new TransactionRejectedEvent(command.account, "Invalid deposit amount: " + command.amount);
            }
            return new AmountAddedOrRemovedEvent(command.account, command.amount);
        }

        public BankEvent process(final WithdrawCommand command) {
            validateAccount(command.account);
            if (command.amount <= 0) {
                return new TransactionRejectedEvent(command.account, "Invalid withdraw amount: " + command.amount);
            }
            if (command.amount > balance) {
                return new TransactionRejectedEvent(command.account, "Withdrawal results in negative account balance: " +
                        "current balance=" + balance + ", attempted withdrawal amount=" + command.amount);
            }
            return new AmountAddedOrRemovedEvent(command.account, -command.amount);
        }

        public BankEvent[] process(final TransferCommand command) {
            validateAccount(command.from);
            if (command.amount <= 0) {
                return new BankEvent[] {new TransactionRejectedEvent(
                        command.from, "Invalid transfer amount: " + command.amount
                )};
            }
            if (command.amount > balance) {
                return new BankEvent[] {new TransactionRejectedEvent(
                        command.from, "Transfer results in negative account balance: "
                        + "current balance=" + balance + ", attempted transfer amount="
                        + command.amount)
                };
            }
            if (!bank.hasAccount(command.to)) {
                return new BankEvent[] {new TransactionRejectedEvent(
                        command.to, "Invalid target account in transfer"
                )};
            }
            return new BankEvent[] {
                    new AmountAddedOrRemovedEvent(command.from, -command.amount),
                    new AmountAddedOrRemovedEvent(command.to, command.amount),
            };
        }

        private void validateAccount(final String account) {
            if (!this.name.equals(account)) {
                throw new IllegalArgumentException("Invalid account: expected=" + name +
                        " but found account=" + account);
            }
        }
    }
}
