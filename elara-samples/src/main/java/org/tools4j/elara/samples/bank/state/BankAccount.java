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
package org.tools4j.elara.samples.bank.state;

import org.tools4j.elara.samples.bank.command.DepositCommand;
import org.tools4j.elara.samples.bank.command.TransferCommand;
import org.tools4j.elara.samples.bank.command.WithdrawCommand;

import static java.util.Objects.requireNonNull;

public interface BankAccount {
    Bank bank();
    String name();
    double balance();

    void deposit(DepositCommand command, BankEventRouter router);
    void withdraw(WithdrawCommand command, BankEventRouter router);
    boolean transfer(TransferCommand command, BankEventRouter router);

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
        public void deposit(final DepositCommand command, final BankEventRouter router) {
            final CharSequence account = command.account();
            final double amount = command.amount();
            validateAccount(account);
            if (amount <= 0) {
                router.routeTransactionRejectedEvent(account, "Invalid deposit amount");
                return;
            }
            router.routeAmountAddedOrRemovedEvent(account, amount);
        }

        @Override
        public void withdraw(final WithdrawCommand command, final BankEventRouter router) {
            final CharSequence account = command.account();
            final double amount = command.amount();
            validateAccount(account);
            if (amount <= 0) {
                router.routeTransactionRejectedEvent(account, "Invalid withdrawal amount");
                return;
            }
            if (amount > balance) {
                router.routeTransactionRejectedEvent(account, "Insufficient funds for given withdrawal amount");
                return;
            }
            router.routeAmountAddedOrRemovedEvent(account, -amount);
        }

        @Override
        public boolean transfer(final TransferCommand command, final BankEventRouter router) {
            final CharSequence from = command.from();
            final CharSequence to = command.to();
            final double amount = command.amount();
            final boolean isFrom = isThisAccount(from);
            final boolean isTo = isThisAccount(to);
            if (!isFrom && !isTo) {
                //coding error, wrong account invoked
                throw new IllegalArgumentException("Invalid account: expected " + name +
                        " but found transfer from " + from + " to " + to);
            }
            if (isFrom && isTo) {
                router.routeTransactionRejectedEvent(name, "Transfer from and to account are identical");
                return false;
            }
            if (amount <= 0) {
                router.routeTransactionRejectedEvent(name, "Invalid transfer amount");
                return false;
            }
            if (isFrom && amount > balance) {
                router.routeTransactionRejectedEvent(name, "Insufficient funds for given transfer amount");
                return false;
            }
            router.routeAmountAddedOrRemovedEvent(name, isFrom ? -amount : amount);
            return true;
        }

        private void validateAccount(final CharSequence account) {
            if (!isThisAccount(account)) {
                //coding error, wrong account invoked
                throw new IllegalArgumentException("Invalid account: expected " + name + " but found " + account);
            }
        }

        private boolean isThisAccount(final CharSequence account) {
            return equalCharSequences(name, account);
        }

        private boolean equalCharSequences(final CharSequence ch1, final CharSequence ch2) {
            if (ch1 == ch2) {
                return true;
            }
            if (ch1 == null || ch2 == null) {
                return false;
            }
            final int length;
            if ((length = ch1.length()) != ch2.length()) {
                return false;
            }
            for (int i = 0; i < length; i++) {
                if (ch1.charAt(i) != ch2.charAt(i)) {
                    return false;
                }
            }
            return true;
        }
    }
}
