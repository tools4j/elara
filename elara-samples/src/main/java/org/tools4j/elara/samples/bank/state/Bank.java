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
package org.tools4j.elara.samples.bank.state;

import org.tools4j.elara.samples.bank.command.CreateAccountCommand;
import org.tools4j.elara.samples.bank.event.AccountCreatedEvent;
import org.tools4j.elara.samples.bank.event.AccountCreationRejectedEvent;
import org.tools4j.elara.samples.bank.event.BankEvent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface Bank {
    Set<String> accounts();
    boolean hasAccount(String name);
    BankAccount account(String name);

    BankEvent process(CreateAccountCommand command);

    interface Mutable extends Bank {
        @Override
        BankAccount.Mutable account(String name);
        BankAccount.Mutable openAccount(final String name);
        BankAccount.Mutable closeAccount(final String name);
    }

    class Default implements Mutable {
        private final Map<String, BankAccount.Mutable> accountByName = new LinkedHashMap<>();

        @Override
        public Set<String> accounts() {
            return Collections.unmodifiableSet(accountByName.keySet());
        }

        @Override
        public boolean hasAccount(final String name) {
            return accountByName.containsKey(name);
        }

        @Override
        public BankAccount.Mutable account(final String name) {
            final BankAccount.Mutable account = accountByName.get(name);
            if (account != null) {
                return account;
            }
            throw new IllegalArgumentException("No such account: " + name);
        }

        @Override
        public BankAccount.Mutable openAccount(final String name) {
            final BankAccount.Mutable account = new BankAccount.Default(this, name);
            if (accountByName.putIfAbsent(name, account) != null) {
                throw new IllegalArgumentException("Account already exists: " + name);
            }
            return account;
        }

        @Override
        public BankAccount.Mutable closeAccount(final String name) {
            final BankAccount.Mutable account = accountByName.remove(name);
            if (account != null) {
                return account;
            }
            throw new IllegalArgumentException("No such account: " + name);
        }

        @Override
        public BankEvent process(final CreateAccountCommand command) {
            final String account = command.name;
            if (hasAccount(account)) {
                return new AccountCreationRejectedEvent(account, "account with this name already exists");
            }
            return new AccountCreatedEvent(account);
        }
    }
}
